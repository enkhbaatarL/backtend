package com.sk.domain.mi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.newsletter.JsonPatchOpReq;
import com.sk.domain.mi.dto.request.newsletter.RevisionSaveReq;
import com.sk.domain.mi.dto.response.newsletter.ContentSnapshotRes;
import com.sk.domain.mi.dto.response.newsletter.DeltaItemRes;
import com.sk.domain.mi.dto.response.newsletter.JsonPatchOpRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionDraftRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionFlowGuideRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionItemRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionListRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionOrOriginalDetailRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionReplayRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionSaveRes;
import com.sk.domain.mi.service.newsletter.SnapshotCanonicalizer;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevision;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevisionDeltaHistory;
import com.sk.domain.shared.entity.master.tmd.NewsletterUserRevisionSnapshotHistory;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.newsletter.RevisionEditStatusCode;
import com.sk.domain.shared.enums.newsletter.RevisionSaveRequestType;
import com.sk.domain.shared.enums.newsletter.RevisionSnapshotBaseType;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterUserRevisionDeltaHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterUserRevisionRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterUserRevisionSnapshotHistoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiNewsletterRevisionService {

    private static final int ORIGINAL_SNAPSHOT_REVISION_NO = 0;
    private static final String NEWS_EVENT_SHOW_IN_CHART_FIELD = "showInChart";

    private final NewsletterIssueOriginalRepository newsletterIssueOriginalRepository;
    private final NewsletterUserRevisionRepository newsletterUserRevisionRepository;
    private final NewsletterUserRevisionDeltaHistoryRepository revisionDeltaHistoryRepository;
    private final NewsletterUserRevisionSnapshotHistoryRepository revisionSnapshotHistoryRepository;
    private final ObjectMapper objectMapper;
    private final SnapshotCanonicalizer snapshotCanonicalizer;

    public RevisionFlowGuideRes getRevisionFlowGuide(Integer newsletterId, Integer issueNo) {
        validateNewsletterIssue(newsletterId, issueNo);

        return new RevisionFlowGuideRes(
            RevisionEditStatusCode.EDIT_DRAFT.name(),
            true,
            new RevisionFlowGuideRes.Endpoints(
                new RevisionFlowGuideRes.Draft(
                    "PUT /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/draft",
                    true,
                    "revisionNo 존재 시 일치 필수"
                ),
                new RevisionFlowGuideRes.Save(
                    "PUT /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/save",
                    true,
                    RevisionEditStatusCode.EDIT_SAVED.name()
                ),
                new RevisionFlowGuideRes.DraftLoad(
                    "GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/draft",
                    "리비전 1개 정책으로 revisionId path 생략"
                ),
                new RevisionFlowGuideRes.History(
                    "GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/{revisionId}/changes",
                    "GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/{revisionId}/changes/{seq}"
                ),
                new RevisionFlowGuideRes.SendMail(
                    "POST /api/v1/mi/newsletter/{newsletterId}/{issueNo}/send-mail",
                    "EDIT_SAVED 상태에서만 허용"
                )
            )
        );
    }

    public RevisionOrOriginalDetailRes getRevisionOrOriginalDetail(
        Integer newsletterId,
        Integer issueNo,
        User actorUser
    ) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(newsletterId, issueNo, actorUser);
        return getRevisionOrOriginalDetailForAccessibleIssue(issueOriginal, actorUser.getUserNo());
    }

    public RevisionOrOriginalDetailRes getRevisionOrOriginalDetailForAccessibleIssue(
        NewsletterIssueOriginal issueOriginal,
        Integer ownerUserNo
    ) {
        validateIssueOriginal(issueOriginal);
        validateOwnerUserNo(ownerUserNo);

        Integer newsletterId = issueOriginal.getNewsletterMaster().getNewsletterMasterId();
        Integer issueNo = issueOriginal.getIssueNo();
        NewsletterUserRevision revision = findOwnedRevision(issueOriginal, ownerUserNo);

        if (revision == null) {
            JsonNode originalSnapshot = snapshotCanonicalizer.canonicalize(deepCopy(issueOriginal.getOriginalSnapshotJson()));
            return new RevisionOrOriginalDetailRes(
                newsletterId,
                issueNo,
                null,
                "NONE",
                null,
                false,
                null,
                new ContentSnapshotRes(ORIGINAL_SNAPSHOT_REVISION_NO, originalSnapshot, issueOriginal.getCreateDt()),
                0,
                List.of()
            );
        }

        // 저장된 최근 스냅샷을 그대로 반환 (delta replay 없음)
        JsonNode snapshotContent;
        LocalDateTime snapshotSavedAt;
        Long baseDeltaNo;

        if (revision.getSnapshotBaseType() == RevisionSnapshotBaseType.REVISION_SNAPSHOT && revision.getSnapshotNo() != null) {
            NewsletterUserRevisionSnapshotHistory snapshot = revisionSnapshotHistoryRepository
                .findByNewsletterUserRevisionAndSnapshotNoAndIsDeletedFalse(revision, revision.getSnapshotNo())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "REVISION_NOT_FOUND"));
            snapshotContent = snapshotCanonicalizer.canonicalize(deepCopy(snapshot.getSnapshotJson()));
            snapshotSavedAt = snapshot.getCreateDt();
            baseDeltaNo = snapshot.getBaseDeltaNo();
        } else {
            snapshotContent = snapshotCanonicalizer.canonicalize(deepCopy(issueOriginal.getOriginalSnapshotJson()));
            snapshotSavedAt = issueOriginal.getCreateDt();
            baseDeltaNo = 0L;
        }

        // 최근 스냅샷 이후 SAVE 타입 delta만 조회
        List<NewsletterUserRevisionDeltaHistory> saveDeltaHistories =
            revisionDeltaHistoryRepository.findByNewsletterUserRevisionAndRequestTypeAndDeltaNoGreaterThanAndIsDeletedFalseOrderByDeltaNoAsc(
                revision, RevisionSaveRequestType.SAVE, baseDeltaNo
            );

        List<JsonPatchOpRes> latestDeltaChanges = flattenDeltaChanges(saveDeltaHistories);
        boolean hasDraft = hasActiveDraft(revision);

        LocalDateTime latestSavedAt = revision.getFinalSavedAt() != null
            ? revision.getFinalSavedAt()
            : (revision.getDraftSavedAt() != null ? revision.getDraftSavedAt() : snapshotSavedAt);

        return new RevisionOrOriginalDetailRes(
            newsletterId,
            issueNo,
            revision.getNewsletterUserRevisionId(),
            revision.getEditStatus().name(),
            revision.getRevisionNo().intValue(),
            hasDraft,
            hasDraft ? revision.getRevisionNo().intValue() : null,
            new ContentSnapshotRes(revision.getRevisionNo().intValue(), snapshotContent, latestSavedAt),
            saveDeltaHistories.size(),
            latestDeltaChanges
        );
    }

    public RevisionListRes getRevisionChanges(
        Integer newsletterId,
        Integer issueNo,
        Long revisionId,
        User actorUser
    ) {
        NewsletterUserRevision revision = getOwnedRevision(newsletterId, issueNo, revisionId, actorUser);

        List<NewsletterUserRevisionDeltaHistory> histories =
            revisionDeltaHistoryRepository.findTop20ByNewsletterUserRevisionAndRequestTypeAndIsDeletedFalseOrderByDeltaNoDesc(
                revision, RevisionSaveRequestType.SAVE
            );

        List<RevisionItemRes> items = histories.stream()
            .map(history -> new RevisionItemRes(
                history.getDeltaNo(),
                toApiRevisionType(history.getRequestType()),
                history.getModifyDt(),
                history.getModifyUserId()
            ))
            .toList();

        if (items.isEmpty()) {
            return new RevisionListRes(List.of(), false, null);
        }

        Long nextCursor = items.get(items.size() - 1).seq();
        boolean hasNext = revisionDeltaHistoryRepository
            .countByNewsletterUserRevisionAndRequestTypeAndDeltaNoLessThanAndIsDeletedFalse(
                revision, RevisionSaveRequestType.SAVE, nextCursor
            ) > 0;

        return new RevisionListRes(items, hasNext, hasNext ? nextCursor : null);
    }

    public RevisionReplayRes getRevisionChangeBySeq(
        Integer newsletterId,
        Integer issueNo,
        Long revisionId,
        Long seq,
        User actorUser
    ) {
        validateSeq(seq);

        NewsletterUserRevision revision = getOwnedRevision(newsletterId, issueNo, revisionId, actorUser);
        NewsletterIssueOriginal issueOriginal = revision.getIssueOriginal();

        revisionDeltaHistoryRepository.findByNewsletterUserRevisionAndDeltaNoAndIsDeletedFalse(revision, seq)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "REVISION_NOT_FOUND"));

        ReplayContext replayContext = resolveReplayContextForSeq(issueOriginal, revision, seq);

        List<NewsletterUserRevisionDeltaHistory> histories =
            revisionDeltaHistoryRepository.findByNewsletterUserRevisionAndDeltaNoGreaterThanAndDeltaNoLessThanEqualAndIsDeletedFalseOrderByDeltaNoAsc(
                revision,
                replayContext.baseDeltaNo(),
                seq
            );

        List<DeltaItemRes> deltaItems = histories.stream()
            .map(history -> new DeltaItemRes(history.getDeltaNo(), toJsonPatchOps(history.getDeltaJson())))
            .toList();

        Long fromSeq = histories.isEmpty() ? seq : histories.get(0).getDeltaNo();

        return new RevisionReplayRes(
            new RevisionReplayRes.BaseSnapshot(replayContext.baseDeltaNo(), deepCopy(replayContext.baseContent())),
            fromSeq,
            seq,
            deltaItems
        );
    }

    public RevisionDraftRes getDraft(Integer newsletterId, Integer issueNo, User actorUser) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(newsletterId, issueNo, actorUser);
        NewsletterUserRevision revision = findOwnedRevision(issueOriginal, actorUser.getUserNo());

        if (revision == null || !hasActiveDraft(revision)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "REVISION_NOT_FOUND");
        }

        ReplayContext replayContext = buildReplayContext(issueOriginal, revision);
        JsonNode content = snapshotCanonicalizer.canonicalize(
            applyDeltaHistories(deepCopy(replayContext.baseContent()), replayContext.histories())
        );

        return new RevisionDraftRes(
            revision.getRevisionNo().intValue(),
            content,
            "AUTOSAVE",
            revision.getDraftSavedAt(),
            revision.getModifyUserId()
        );
    }

    @Transactional
    public RevisionSaveRes saveDraft(
        Integer newsletterId,
        Integer issueNo,
        RevisionSaveReq request,
        User actorUser
    ) {
        return save(newsletterId, issueNo, request, actorUser, RevisionSaveRequestType.DRAFT);
    }

    @Transactional
    public RevisionSaveRes saveFinal(
        Integer newsletterId,
        Integer issueNo,
        RevisionSaveReq request,
        User actorUser
    ) {
        return save(newsletterId, issueNo, request, actorUser, RevisionSaveRequestType.SAVE);
    }

    private RevisionSaveRes save(
        Integer newsletterId,
        Integer issueNo,
        RevisionSaveReq request,
        User actorUser,
        RevisionSaveRequestType requestType
    ) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(newsletterId, issueNo, actorUser);

        RevisionSaveReq normalizedRequest = normalizeRequest(request);
        validatePatchOps(normalizedRequest.deltas());

        NewsletterUserRevision revision = findOwnedRevision(issueOriginal, actorUser.getUserNo());
        LocalDateTime now = LocalDateTime.now();

        if (revision == null) {
            return createFirstRevision(issueOriginal, actorUser, normalizedRequest, requestType, now);
        }

        Long expectedRevisionNo = toLong(normalizedRequest.revisionNo());
        if (expectedRevisionNo != null && !expectedRevisionNo.equals(revision.getRevisionNo())) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "REVISION_VERSION_CONFLICT");
        }

        ReplayContext replayContext = buildReplayContext(issueOriginal, revision);
        JsonNode currentContent = applyDeltaHistories(deepCopy(replayContext.baseContent()), replayContext.histories());
        JsonNode mergedContent = applyPatchOps(currentContent, normalizedRequest.deltas());

        long nextRevisionNo = revision.getRevisionNo() + 1;
        long nextDeltaNo = revision.getDeltaNo() + 1;
        boolean snapshotSaved = normalizedRequest.snapshotPayload() != null;

        RevisionEditStatusCode nextStatus = toEditStatus(requestType);
        RevisionSnapshotBaseType nextSnapshotBaseType = snapshotSaved
            ? RevisionSnapshotBaseType.REVISION_SNAPSHOT
            : revision.getSnapshotBaseType();

        ArrayNode deltaJson = toDeltaJson(normalizedRequest.deltas());
        NewsletterUserRevisionDeltaHistory deltaHistory = NewsletterUserRevisionDeltaHistory.create(
            revision,
            nextDeltaNo,
            expectedRevisionNo,
            requestType,
            deltaJson
        );
        revisionDeltaHistoryRepository.save(deltaHistory);

        Long snapshotNoToPersist = nextSnapshotBaseType == RevisionSnapshotBaseType.REVISION_SNAPSHOT
            ? Long.valueOf(nextRevisionNo)
            : revision.getSnapshotNo();

        if (expectedRevisionNo != null) {
            int updatedCount = newsletterUserRevisionRepository.updateWithExpectedRevisionNo(
                revision.getNewsletterUserRevisionId(),
                actorUser.getUserNo(),
                expectedRevisionNo,
                nextStatus,
                nextSnapshotBaseType,
                snapshotNoToPersist,
                nextDeltaNo,
                requestType == RevisionSaveRequestType.DRAFT,
                requestType == RevisionSaveRequestType.SAVE,
                actorUser.getUserLoginId(),
                now
            );
            if (updatedCount == 0) {
                throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "REVISION_VERSION_CONFLICT");
            }
        } else {
            revision.applySave(nextStatus, nextRevisionNo, nextDeltaNo, snapshotSaved,
                nextRevisionNo, now);
            newsletterUserRevisionRepository.save(revision);
        }

        if (snapshotSaved) {
            JsonNode snapshotJson = deepCopy(normalizedRequest.snapshotPayload() == null
                ? mergedContent
                : normalizedRequest.snapshotPayload());
            NewsletterUserRevisionSnapshotHistory snapshotHistory = NewsletterUserRevisionSnapshotHistory.create(
                revision,
                nextRevisionNo,
                nextDeltaNo,
                snapshotJson
            );
            revisionSnapshotHistoryRepository.save(snapshotHistory);
        }

        LocalDateTime savedAt = requestType == RevisionSaveRequestType.SAVE
            ? now
            : revision.getFinalSavedAt();
        LocalDateTime lastAutoSavedAt = requestType == RevisionSaveRequestType.DRAFT
            ? now
            : null;

        return new RevisionSaveRes(
            revision.getNewsletterUserRevisionId(),
            (int) nextRevisionNo,
            nextStatus.name(),
            savedAt,
            lastAutoSavedAt,
            snapshotSaved ? Boolean.TRUE : null
        );
    }

    private RevisionSaveRes createFirstRevision(
        NewsletterIssueOriginal issueOriginal,
        User actorUser,
        RevisionSaveReq request,
        RevisionSaveRequestType requestType,
        LocalDateTime now
    ) {
        long revisionNo = 1L;
        long deltaNo = 1L;
        RevisionEditStatusCode status = toEditStatus(requestType);

        JsonNode mergedContent = applyPatchOps(deepCopy(issueOriginal.getOriginalSnapshotJson()), request.deltas());

        NewsletterUserRevision revision = NewsletterUserRevision.create(
            issueOriginal,
            actorUser,
            status,
            revisionNo,
            deltaNo,
            now
        );
        revision = newsletterUserRevisionRepository.save(revision);

        NewsletterUserRevisionDeltaHistory deltaHistory = NewsletterUserRevisionDeltaHistory.create(
            revision,
            deltaNo,
            toLong(request.revisionNo()),
            requestType,
            toDeltaJson(request.deltas())
        );
        revisionDeltaHistoryRepository.save(deltaHistory);

        boolean snapshotSaved = request.snapshotPayload() != null;
        if (snapshotSaved) {
            NewsletterUserRevisionSnapshotHistory snapshotHistory = NewsletterUserRevisionSnapshotHistory.create(
                revision,
                revisionNo,
                deltaNo,
                deepCopy(request.snapshotPayload())
            );
            revisionSnapshotHistoryRepository.save(snapshotHistory);
            revision.applySave(status, revisionNo, deltaNo, true, revisionNo, now);
            newsletterUserRevisionRepository.save(revision);
        }

        return new RevisionSaveRes(
            revision.getNewsletterUserRevisionId(),
            (int) revisionNo,
            status.name(),
            requestType == RevisionSaveRequestType.SAVE ? now : null,
            requestType == RevisionSaveRequestType.DRAFT ? now : null,
            snapshotSaved ? Boolean.TRUE : null
        );
    }

    private ReplayContext buildReplayContext(NewsletterIssueOriginal issueOriginal, NewsletterUserRevision revision) {
        if (revision.getSnapshotBaseType() == RevisionSnapshotBaseType.REVISION_SNAPSHOT && revision.getSnapshotNo() != null) {
            NewsletterUserRevisionSnapshotHistory snapshot = revisionSnapshotHistoryRepository
                .findByNewsletterUserRevisionAndSnapshotNoAndIsDeletedFalse(revision, revision.getSnapshotNo())
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "REVISION_NOT_FOUND"));

            List<NewsletterUserRevisionDeltaHistory> deltas =
                revisionDeltaHistoryRepository.findByNewsletterUserRevisionAndDeltaNoGreaterThanAndIsDeletedFalseOrderByDeltaNoAsc(
                    revision,
                    snapshot.getBaseDeltaNo()
                );
            return new ReplayContext(snapshot.getSnapshotNo(), snapshot.getBaseDeltaNo(), deepCopy(snapshot.getSnapshotJson()), snapshot.getCreateDt(), deltas);
        }

        List<NewsletterUserRevisionDeltaHistory> deltas =
            revisionDeltaHistoryRepository.findByNewsletterUserRevisionAndDeltaNoGreaterThanAndIsDeletedFalseOrderByDeltaNoAsc(
                revision,
                0L
            );
        return new ReplayContext(
            (long) ORIGINAL_SNAPSHOT_REVISION_NO,
            0L,
            deepCopy(issueOriginal.getOriginalSnapshotJson()),
            issueOriginal.getCreateDt(),
            deltas
        );
    }

    private ReplayContext resolveReplayContextForSeq(
        NewsletterIssueOriginal issueOriginal,
        NewsletterUserRevision revision,
        Long seq
    ) {
        if (revision.getSnapshotBaseType() == RevisionSnapshotBaseType.REVISION_SNAPSHOT && revision.getSnapshotNo() != null) {
            NewsletterUserRevisionSnapshotHistory snapshot = revisionSnapshotHistoryRepository
                .findByNewsletterUserRevisionAndSnapshotNoAndIsDeletedFalse(revision, revision.getSnapshotNo())
                .orElse(null);

            if (snapshot != null && snapshot.getBaseDeltaNo() <= seq) {
                return new ReplayContext(snapshot.getSnapshotNo(), snapshot.getBaseDeltaNo(), deepCopy(snapshot.getSnapshotJson()), snapshot.getCreateDt(), List.of());
            }
        }

        return new ReplayContext(0L, 0L, deepCopy(issueOriginal.getOriginalSnapshotJson()), issueOriginal.getCreateDt(), List.of());
    }

    private NewsletterIssueOriginal getAccessibleIssue(Integer newsletterId, Integer issueNo, User actorUser) {
        validateActorUser(actorUser);
        validateNewsletterIssue(newsletterId, issueNo);

        Integer companyNo = actorUser.getCustomerCompany().getCustomerCompanyNo();
        return newsletterIssueOriginalRepository
            .findAccessibleIssue(newsletterId, issueNo, companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));
    }

    private NewsletterUserRevision getOwnedRevision(
        Integer newsletterId,
        Integer issueNo,
        Long revisionId,
        User actorUser
    ) {
        validateActorUser(actorUser);
        validateNewsletterIssue(newsletterId, issueNo);

        if (revisionId == null || revisionId <= 0L) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "revisionId is invalid");
        }

        return newsletterUserRevisionRepository
            .findOwnedRevision(revisionId, actorUser.getUserNo(), newsletterId, issueNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "REVISION_NOT_FOUND"));
    }

    private NewsletterUserRevision findOwnedRevision(NewsletterIssueOriginal issueOriginal, Integer ownerUserNo) {
        return newsletterUserRevisionRepository
            .findByIssueOriginalAndOwnerUserUserNoAndIsDeletedFalse(issueOriginal, ownerUserNo)
            .orElse(null);
    }

    private void validateActorUser(User actorUser) {
        if (actorUser == null || actorUser.getUserNo() == null || actorUser.getCustomerCompany() == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "actor user is required");
        }
    }

    private void validateIssueOriginal(NewsletterIssueOriginal issueOriginal) {
        if (issueOriginal == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "issueOriginal is required");
        }
    }

    private void validateOwnerUserNo(Integer ownerUserNo) {
        if (ownerUserNo == null || ownerUserNo <= 0) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "owner user is required");
        }
    }

    private void validateNewsletterIssue(Integer newsletterId, Integer issueNo) {
        if (newsletterId == null || newsletterId <= 0 || issueNo == null || issueNo <= 0) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "newsletterId/issueNo is invalid");
        }
    }

    private void validateSeq(Long seq) {
        if (seq == null || seq <= 0L) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "seq is invalid");
        }
    }

    private RevisionSaveReq normalizeRequest(RevisionSaveReq request) {
        if (request == null) {
            return new RevisionSaveReq(null, List.of(), null);
        }
        List<JsonPatchOpReq> deltas = request.deltas() == null ? List.of() : request.deltas();
        return new RevisionSaveReq(request.revisionNo(), deltas, request.snapshotPayload());
    }

    private void validatePatchOps(List<JsonPatchOpReq> patchOps) {
        for (JsonPatchOpReq patchOp : patchOps) {
            if (patchOp == null || StringUtils.isBlank(patchOp.op()) || StringUtils.isBlank(patchOp.path())) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "invalid patch op");
            }
        }
    }

    private ArrayNode toDeltaJson(List<JsonPatchOpReq> patchOps) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (JsonPatchOpReq patchOp : patchOps) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("op", patchOp.op());
            objectNode.put("path", patchOp.path());
            objectNode.set("value", patchOp.value() == null ? NullNode.instance : patchOp.value());
            arrayNode.add(objectNode);
        }
        return arrayNode;
    }

    private List<JsonPatchOpRes> toJsonPatchOps(JsonNode deltaJson) {
        if (!(deltaJson instanceof ArrayNode arrayNode)) {
            return List.of();
        }

        List<JsonPatchOpRes> result = new ArrayList<>();
        arrayNode.forEach(node -> result.add(new JsonPatchOpRes(
            node.path("op").asText(),
            node.path("path").asText(),
            node.path("value") instanceof MissingNode ? null : node.path("value")
        )));
        return result;
    }

    private JsonNode applyDeltaHistories(JsonNode baseContent, List<NewsletterUserRevisionDeltaHistory> histories) {
        JsonNode current = baseContent;
        for (NewsletterUserRevisionDeltaHistory history : histories) {
            current = applyPatchOps(current, toPatchOps(history.getDeltaJson()));
        }
        return current;
    }

    private List<JsonPatchOpReq> toPatchOps(JsonNode deltaJson) {
        if (!(deltaJson instanceof ArrayNode arrayNode)) {
            return List.of();
        }
        List<JsonPatchOpReq> ops = new ArrayList<>();
        arrayNode.forEach(node -> ops.add(new JsonPatchOpReq(
            node.path("op").asText(),
            node.path("path").asText(),
            node.path("value") instanceof MissingNode ? null : node.path("value")
        )));
        return ops;
    }

    private JsonNode applyPatchOps(JsonNode source, List<JsonPatchOpReq> patchOps) {
        JsonNode current = deepCopy(source);
        for (JsonPatchOpReq patchOp : patchOps) {
            current = applyPatchOp(current, patchOp);
        }
        return current;
    }

    private JsonNode applyPatchOp(JsonNode source, JsonPatchOpReq patchOp) {
        String op = patchOp.op().toLowerCase();
        return switch (op) {
            case "add" -> applyAdd(source, patchOp.path(), patchOp.value());
            case "replace" -> applyReplace(source, patchOp.path(), patchOp.value());
            case "remove" -> applyRemove(source, patchOp.path());
            default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "unsupported patch op: " + patchOp.op());
        };
    }

    private JsonNode applyAdd(JsonNode source, String path, JsonNode value) {
        return setValueByPath(source, path, value == null ? NullNode.instance : value, true);
    }

    private JsonNode applyReplace(JsonNode source, String path, JsonNode value) {
        return setValueByPath(source, path, value == null ? NullNode.instance : value, false);
    }

    private JsonNode applyRemove(JsonNode source, String path) {
        if ("/".equals(path) || "".equals(path)) {
            return NullNode.instance;
        }

        List<String> tokens = splitPointer(path);
        JsonNode root = deepCopy(source);
        JsonNode parent = navigateParent(root, tokens);
        String lastToken = tokens.get(tokens.size() - 1);

        if (parent instanceof ObjectNode objectNode) {
            if (!objectNode.has(lastToken)) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch path not found: " + path);
            }
            objectNode.remove(lastToken);
            return root;
        }

        if (parent instanceof ArrayNode arrayNode) {
            int index = parseArrayIndex(lastToken, path);
            if (index < 0 || index >= arrayNode.size()) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch index out of range: " + path);
            }
            arrayNode.remove(index);
            return root;
        }

        throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch parent is not a container: " + path);
    }

    private JsonNode setValueByPath(JsonNode source, String path, JsonNode value, boolean allowCreate) {
        if ("/".equals(path) || "".equals(path)) {
            return deepCopy(value);
        }

        List<String> tokens = splitPointer(path);
        JsonNode root = deepCopy(source);
        JsonNode parent = navigateParent(root, tokens);
        String lastToken = tokens.get(tokens.size() - 1);

        if (parent instanceof ObjectNode objectNode) {
            if (!allowCreate && !objectNode.has(lastToken)) {
                if (canBackfillMissingField(tokens, lastToken)) {
                    objectNode.set(lastToken, deepCopy(value));
                    return root;
                }
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch path not found: " + path);
            }
            objectNode.set(lastToken, deepCopy(value));
            return root;
        }

        if (parent instanceof ArrayNode arrayNode) {
            if ("-".equals(lastToken) && allowCreate) {
                arrayNode.add(deepCopy(value));
                return root;
            }

            int index = parseArrayIndex(lastToken, path);
            if (allowCreate) {
                if (index < 0 || index > arrayNode.size()) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch index out of range: " + path);
                }
                arrayNode.insert(index, deepCopy(value));
                return root;
            }

            if (index < 0 || index >= arrayNode.size()) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch index out of range: " + path);
            }
            arrayNode.set(index, deepCopy(value));
            return root;
        }

        throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch parent is not a container: " + path);
    }

    private boolean canBackfillMissingField(List<String> tokens, String lastToken) {
        if (!NEWS_EVENT_SHOW_IN_CHART_FIELD.equals(lastToken)) {
            return false;
        }
        if (tokens.size() < 4) {
            return false;
        }
        return "relatedNews".equals(tokens.get(tokens.size() - 4))
            && "newsEventItems".equals(tokens.get(tokens.size() - 3));
    }

    private JsonNode navigateParent(JsonNode root, List<String> tokens) {
        if (tokens.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch path is invalid");
        }

        JsonNode current = root;
        for (int i = 0; i < tokens.size() - 1; i++) {
            String token = tokens.get(i);

            if (current instanceof ObjectNode objectNode) {
                JsonNode child = objectNode.get(token);
                if (child == null) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch path not found: /" + String.join("/", tokens));
                }
                current = child;
                continue;
            }

            if (current instanceof ArrayNode arrayNode) {
                int index = parseArrayIndex(token, "/" + String.join("/", tokens));
                if (index < 0 || index >= arrayNode.size()) {
                    throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch index out of range: /" + String.join("/", tokens));
                }
                current = arrayNode.get(index);
                continue;
            }

            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch path not found: /" + String.join("/", tokens));
        }

        return current;
    }

    private List<String> splitPointer(String path) {
        if (StringUtils.isBlank(path) || !path.startsWith("/")) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "patch path must start with '/': " + path);
        }
        if ("/".equals(path)) {
            return Collections.emptyList();
        }

        String[] segments = path.substring(1).split("/");
        List<String> tokens = new ArrayList<>(segments.length);
        for (String segment : segments) {
            tokens.add(segment.replace("~1", "/").replace("~0", "~"));
        }
        return tokens;
    }

    private int parseArrayIndex(String token, String path) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "invalid array index: " + path);
        }
    }

    private List<JsonPatchOpRes> flattenDeltaChanges(List<NewsletterUserRevisionDeltaHistory> histories) {
        List<JsonPatchOpRes> changes = new ArrayList<>();
        for (NewsletterUserRevisionDeltaHistory history : histories) {
            changes.addAll(toJsonPatchOps(history.getDeltaJson()));
        }
        return changes;
    }

    private RevisionEditStatusCode toEditStatus(RevisionSaveRequestType requestType) {
        return requestType == RevisionSaveRequestType.DRAFT
            ? RevisionEditStatusCode.EDIT_DRAFT
            : RevisionEditStatusCode.EDIT_SAVED;
    }

    private boolean hasActiveDraft(NewsletterUserRevision revision) {
        return revision.getEditStatus() == RevisionEditStatusCode.EDIT_DRAFT
            && revision.getDraftSavedAt() != null;
    }

    private String toApiRevisionType(RevisionSaveRequestType requestType) {
        return requestType == RevisionSaveRequestType.DRAFT ? "AUTOSAVE" : "FINAL";
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    private JsonNode deepCopy(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return NullNode.instance;
        }
        return jsonNode.deepCopy();
    }

    private record ReplayContext(
        Long baseRevisionNo,
        Long baseDeltaNo,
        JsonNode baseContent,
        LocalDateTime baseSnapshotSavedAt,
        List<NewsletterUserRevisionDeltaHistory> histories
    ) {
    }
}

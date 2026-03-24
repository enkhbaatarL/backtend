package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.newsletter.ContentSnapshotRes;
import com.sk.domain.mi.dto.response.newsletter.JsonPatchOpRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailV2Res;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailV2Res.RevisionStateRes;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssueOriginalSnapshotRes;
import com.sk.domain.mi.dto.response.newsletter.RevisionOrOriginalDetailRes;
import com.sk.domain.mi.service.MiNewsletterRevisionService;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterSubscriptionRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterDetailV2Service {

    private static final String ORIGINAL_SOURCE = "ORIGINAL";
    private static final String NEWSLETTER_NOT_FOUND = "NEWSLETTER_NOT_FOUND";
    private static final String ORIGINAL_SNAPSHOT_NOT_FOUND = "NEWSLETTER_ORIGINAL_SNAPSHOT_NOT_FOUND";

    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterSubscriptionRepository newsletterSubscriptionRepository;
    private final UserRepository userRepository;
    private final MiNewsletterRevisionService revisionService;
    private final SnapshotCanonicalizer snapshotCanonicalizer;

    public NewsletterDetailV2Res getNewsletterDetail(Integer companyNo, Integer userNo, Integer newsletterId, Integer issueNo) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(companyNo, newsletterId, issueNo);

        RevisionStateRes revision = null;
        ContentSnapshotRes latestSnapshot = new ContentSnapshotRes(
            0,
            snapshotCanonicalizer.canonicalize(issueOriginal.getOriginalSnapshotJson()),
            issueOriginal.getCreateDt()
        );
        Integer deltaCount = 0;
        List<JsonPatchOpRes> deltaChanges = List.of();

        if (userNo != null) {
            User actorUser = userRepository.findByUserNoAndIsDeletedFalse(userNo).orElse(null);
            if (actorUser != null) {
                RevisionOrOriginalDetailRes detail =
                    revisionService.getRevisionOrOriginalDetailForAccessibleIssue(issueOriginal, actorUser.getUserNo());
                latestSnapshot = detail.latestSnapshot();
                deltaCount = detail.deltaCount();
                deltaChanges = detail.deltaChanges();

                if (detail.revisionId() != null) {
                    revision = new RevisionStateRes(
                        detail.revisionId(),
                        detail.editStatus(),
                        detail.revisionNo(),
                        detail.hasDraft(),
                        detail.latestDraftRevisionNo()
                    );
                }
            }
        }

        boolean subscribed = userNo != null && newsletterSubscriptionRepository
            .findByNewsletterMasterAndUserUserNoAndIsDeletedFalse(issueOriginal.getNewsletterMaster(), userNo)
            .map(subscription -> Boolean.TRUE.equals(subscription.getIsSubscribed()))
            .orElse(false);

        return new NewsletterDetailV2Res(
            newsletterId,
            issueNo,
            issueOriginal.getPublishDateTime(),
            subscribed,
            revision,
            latestSnapshot,
            deltaCount,
            deltaChanges
        );
    }

    public NewsletterIssueOriginalSnapshotRes getOriginalSnapshot(Integer companyNo, Integer newsletterId, Integer issueNo) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(companyNo, newsletterId, issueNo);
        JsonNode snapshot = issueOriginal.getOriginalSnapshotJson();

        if (snapshot == null || snapshot.isNull()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, ORIGINAL_SNAPSHOT_NOT_FOUND);
        }

        return new NewsletterIssueOriginalSnapshotRes(
            newsletterId,
            issueNo,
            ORIGINAL_SOURCE,
            snapshotCanonicalizer.canonicalize(snapshot)
        );
    }

    private NewsletterIssueOriginal getAccessibleIssue(Integer companyNo, Integer newsletterId, Integer issueNo) {
        validateRequest(companyNo, newsletterId, issueNo);

        return issueOriginalRepository.findAccessibleIssue(newsletterId, issueNo, companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, NEWSLETTER_NOT_FOUND));
    }

    private void validateRequest(Integer companyNo, Integer newsletterId, Integer issueNo) {
        if (companyNo == null || companyNo <= 0 || newsletterId == null || newsletterId <= 0 || issueNo == null || issueNo <= 0) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "companyNo/newsletterId/issueNo is invalid");
        }
    }
}

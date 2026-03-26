package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterIssueOriginalService {

    private static final String NEWSLETTER_NOT_FOUND = "NEWSLETTER_NOT_FOUND";
    private static final String PUBLISH_HISTORY_MASTER_MISMATCH = "PUBLISH_HISTORY_MASTER_MISMATCH";
    private static final String NEWSLETTER_PUBLISH_HISTORY_NOT_FOUND = "NEWSLETTER_PUBLISH_HISTORY_NOT_FOUND";
    private static final String NEWSLETTER_PUBLISH_HISTORY_NOT_PUBLISHED = "NEWSLETTER_PUBLISH_HISTORY_NOT_PUBLISHED";
    private static final String NEWSLETTER_PUBLISH_HISTORY_EMAIL_SUBJECT_MISSING = "NEWSLETTER_PUBLISH_HISTORY_EMAIL_SUBJECT_MISSING";

    private final TemplateLoader templateLoader;
    private final BlockDataResolver blockDataResolver;
    private final SnapshotAssembler snapshotAssembler;
    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterPublishHistoryRepository publishHistoryRepository;
    private final NewsletterMasterRepository newsletterMasterRepository;

    public JsonNode assembleSnapshot(NewsletterPublishHistory publishHistory) {
        TemplateTree template = templateLoader.loadByPublishHistory(publishHistory);
        Map<String, ResolvedBlockData> resolvedData = blockDataResolver.resolveAll(template, publishHistory, false);
        return snapshotAssembler.assembleNode(template, resolvedData, publishHistory);
    }

    public NewsletterIssueOriginal getIssueOriginal(Integer newsletterId, Integer issueNo) {
        return issueOriginalRepository.findByNewsletterMasterNewsletterMasterIdAndIssueNoAndIsDeletedFalse(newsletterId, issueNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, NEWSLETTER_NOT_FOUND));
    }

    @Transactional
    public NewsletterIssueOriginal upsertOriginalIssue(Integer newsletterId, Integer issueNo) {
        NewsletterPublishHistory publishHistory = getPublishedHistory(newsletterId, issueNo);
        return upsertOriginalIssue(newsletterId, issueNo, publishHistory);
    }

    @Transactional
    public NewsletterIssueOriginal upsertOriginalIssue(Integer newsletterId, Integer issueNo, Integer publishHistoryId) {
        NewsletterPublishHistory publishHistory = publishHistoryRepository.findById(publishHistoryId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, NEWSLETTER_NOT_FOUND));
        return upsertOriginalIssue(newsletterId, issueNo, publishHistory);
    }

    @Transactional
    public NewsletterIssueOriginal upsertOriginalIssue(
        Integer newsletterId,
        Integer issueNo,
        NewsletterPublishHistory publishHistory
    ) {
        try {
            validatePublishHistory(newsletterId, issueNo, publishHistory);

            JsonNode snapshotNode = assembleSnapshot(publishHistory);
            NewsletterIssueOriginal existingIssueOriginal = issueOriginalRepository
                .findByNewsletterMasterNewsletterMasterIdAndIssueNoAndIsDeletedFalse(newsletterId, issueNo)
                .orElse(null);

            NewsletterIssueOriginal issueOriginal = existingIssueOriginal != null
                ? existingIssueOriginal.updateSnapshot(
                    snapshotNode,
                    publishHistory.getPublishStatusCode(),
                    publishHistory.getPublishDateTime(),
                    publishHistory
                )
                : NewsletterIssueOriginal.create(
                    publishHistory.getNewsletterMaster(),
                    publishHistory,
                    issueNo,
                    publishHistory.getPublishStatusCode(),
                    publishHistory.getPublishDateTime(),
                    snapshotNode
                );

            NewsletterIssueOriginal saved = issueOriginalRepository.save(issueOriginal);
            log.info(
                "Newsletter original snapshot upserted. newsletterId={}, issueNo={}, publishHistoryId={}, originalIssueId={}",
                newsletterId,
                issueNo,
                publishHistory.getNewsletterPublishId(),
                saved.getNewsletterIssueOriginalId()
            );
            return saved;
        } catch (AppErrorException e) {
            log.error(
                "Newsletter original snapshot upsert failed. newsletterId={}, issueNo={}, publishHistoryId={}, reason={}",
                newsletterId,
                issueNo,
                publishHistory != null ? publishHistory.getNewsletterPublishId() : null,
                e.getMessage(),
                e
            );
            throw e;
        } catch (Exception e) {
            log.error(
                "Newsletter original snapshot upsert failed. newsletterId={}, issueNo={}, publishHistoryId={}",
                newsletterId,
                issueNo,
                publishHistory != null ? publishHistory.getNewsletterPublishId() : null,
                e
            );
            throw e;
        }
    }

    public NewsletterPublishHistory getPublishedHistory(Integer newsletterId, Integer issueNo) {
        NewsletterMaster newsletterMaster = newsletterMasterRepository.findById(newsletterId)
            .filter(master -> !Boolean.TRUE.equals(master.getIsDeleted()))
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, NEWSLETTER_NOT_FOUND));

        NewsletterPublishHistory publishHistory = publishHistoryRepository.findByNewsletterMasterAndIssueNo(newsletterMaster, issueNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, NEWSLETTER_PUBLISH_HISTORY_NOT_FOUND));

        validatePublishHistory(newsletterId, issueNo, publishHistory);
        return publishHistory;
    }

    public String getPublishedEmailSubject(Integer newsletterId, Integer issueNo) {
        NewsletterPublishHistory publishHistory = getPublishedHistory(newsletterId, issueNo);
        if (StringUtils.isBlank(publishHistory.getEmailSubject())) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                NEWSLETTER_PUBLISH_HISTORY_EMAIL_SUBJECT_MISSING + ": newsletterId=%d, issueNo=%d".formatted(newsletterId, issueNo)
            );
        }
        return publishHistory.getEmailSubject();
    }

    private void validatePublishHistory(Integer newsletterId, Integer issueNo, NewsletterPublishHistory publishHistory) {
        if (!publishHistory.getNewsletterMaster().getNewsletterMasterId().equals(newsletterId)) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, PUBLISH_HISTORY_MASTER_MISMATCH);
        }
        if (!publishHistory.getIssueNo().equals(issueNo)) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, NEWSLETTER_PUBLISH_HISTORY_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(publishHistory.getIsDeleted())) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, NEWSLETTER_PUBLISH_HISTORY_NOT_FOUND);
        }
        if (publishHistory.getPublishStatusCode() != PublishStatusCode.PUBLISHED) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, NEWSLETTER_PUBLISH_HISTORY_NOT_PUBLISHED);
        }
    }
}

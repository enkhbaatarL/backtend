package com.sk.domain.mi.service.newsletter;

import com.sk.domain.mi.service.newsletter.NewsletterRevisionResetService.NewsletterRevisionResetResult;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NewsletterTriggerMailPreparationService {

    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterRevisionResetService newsletterRevisionResetService;
    private final NewsletterIssueOriginalService newsletterIssueOriginalService;

    public NewsletterIssueOriginal resetRevisionStateAndUpsertOriginalIssue(Integer newsletterId, Integer issueNo) {
        String stage = "FIND_ISSUE_ORIGINAL";
        Long issueOriginalId = null;
        int deletedRevisionCount = 0;
        int deletedDeltaCount = 0;
        int deletedSnapshotCount = 0;

        try {
            NewsletterIssueOriginal issueOriginal = issueOriginalRepository
                .findByNewsletterMasterNewsletterMasterIdAndIssueNoAndIsDeletedFalse(newsletterId, issueNo)
                .orElse(null);

            if (issueOriginal != null) {
                issueOriginalId = issueOriginal.getNewsletterIssueOriginalId();
                stage = "RESET_USER_REVISIONS";

                NewsletterRevisionResetResult resetResult = newsletterRevisionResetService.resetUserRevisionState(issueOriginal);
                deletedRevisionCount = resetResult.deletedRevisionCount();
                deletedDeltaCount = resetResult.deletedDeltaCount();
                deletedSnapshotCount = resetResult.deletedSnapshotCount();
            }

            stage = "UPSERT_ORIGINAL_START";
            log.info(
                "Newsletter trigger-mail original snapshot upsert started. newsletterId={}, issueNo={}, issueOriginalId={}, deletedRevisionCount={}, deletedDeltaCount={}, deletedSnapshotCount={}",
                newsletterId,
                issueNo,
                issueOriginalId,
                deletedRevisionCount,
                deletedDeltaCount,
                deletedSnapshotCount
            );

            NewsletterIssueOriginal saved = newsletterIssueOriginalService.upsertOriginalIssue(newsletterId, issueNo);

            stage = "UPSERT_ORIGINAL_COMPLETE";
            log.info(
                "Newsletter trigger-mail original snapshot upsert completed. newsletterId={}, issueNo={}, issueOriginalId={}, deletedRevisionCount={}, deletedDeltaCount={}, deletedSnapshotCount={}",
                newsletterId,
                issueNo,
                saved.getNewsletterIssueOriginalId(),
                deletedRevisionCount,
                deletedDeltaCount,
                deletedSnapshotCount
            );
            return saved;
        } catch (Exception e) {
            log.error(
                "Newsletter trigger-mail preparation failed. stage={}, newsletterId={}, issueNo={}, issueOriginalId={}, deletedRevisionCount={}, deletedDeltaCount={}, deletedSnapshotCount={}",
                stage,
                newsletterId,
                issueNo,
                issueOriginalId,
                deletedRevisionCount,
                deletedDeltaCount,
                deletedSnapshotCount,
                e
            );
            throw e;
        }
    }
}

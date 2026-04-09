package com.sk.domain.mi.service.newsletter;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.newsletter.NewsletterOriginalSnapshotReplayRes;
import com.sk.domain.mi.service.newsletter.NewsletterTriggerMailPreparationService.NewsletterTriggerMailPreparationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterOriginalSnapshotReplayService {

    private final NewsletterTriggerMailPreparationService newsletterTriggerMailPreparationService;

    public NewsletterOriginalSnapshotReplayRes replayOriginalSnapshot(Integer newsletterId, Integer issueNo) {
        validateRequest(newsletterId, issueNo);

        NewsletterTriggerMailPreparationResult result =
            newsletterTriggerMailPreparationService.resetRevisionStateAndUpsertOriginalIssue(newsletterId, issueNo);

        log.info(
            "Newsletter original snapshot replay completed. newsletterId={}, issueNo={}, issueOriginalId={}, deletedRevisionCount={}, deletedDeltaCount={}, deletedSnapshotCount={}",
            result.newsletterId(),
            result.issueNo(),
            result.issueOriginalId(),
            result.deletedRevisionCount(),
            result.deletedDeltaCount(),
            result.deletedSnapshotCount()
        );

        return new NewsletterOriginalSnapshotReplayRes(
            result.newsletterId(),
            result.issueNo(),
            result.issueOriginalId(),
            result.deletedRevisionCount(),
            result.deletedDeltaCount(),
            result.deletedSnapshotCount(),
            "original snapshot replay completed"
        );
    }

    private void validateRequest(Integer newsletterId, Integer issueNo) {
        if (newsletterId == null || newsletterId <= 0 || issueNo == null || issueNo <= 0) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "newsletterId/issueNo is invalid");
        }
    }
}

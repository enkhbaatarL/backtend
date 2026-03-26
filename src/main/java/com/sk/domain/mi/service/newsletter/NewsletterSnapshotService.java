package com.sk.domain.mi.service.newsletter;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssuePreviewRes;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterSnapshotService {

    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterPublishHistoryRepository publishHistoryRepository;
    private final NewsletterIssueOriginalService newsletterIssueOriginalService;

    public NewsletterIssuePreviewRes generateSnapshot(Integer publishHistoryId, Integer customerCompanyNo) {
        Optional<NewsletterIssueOriginal> issueOriginalOpt =
            issueOriginalRepository.findAccessibleIssueByPublishHistoryId(publishHistoryId, customerCompanyNo);

        if (issueOriginalOpt.isPresent()) {
            NewsletterIssueOriginal issueOriginal = issueOriginalOpt.get();
            return new NewsletterIssuePreviewRes(
                issueOriginal.getNewsletterMaster().getNewsletterMasterId(),
                issueOriginal.getIssueNo(),
                newsletterIssueOriginalService.assembleSnapshot(issueOriginal.getSourceNewsletterPublishHistory())
            );
        }

        // 원본 이슈가 아직 생성되지 않은 경우, 발행 이력에서 직접 스냅샷 조립
        NewsletterPublishHistory publishHistory = getAccessiblePublishHistory(publishHistoryId, customerCompanyNo);
        return new NewsletterIssuePreviewRes(
            publishHistory.getNewsletterMaster().getNewsletterMasterId(),
            publishHistory.getIssueNo(),
            newsletterIssueOriginalService.assembleSnapshot(publishHistory)
        );
    }

    private NewsletterPublishHistory getAccessiblePublishHistory(Integer publishHistoryId, Integer customerCompanyNo) {
        return publishHistoryRepository.findAccessiblePublishedById(publishHistoryId, customerCompanyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));
    }
}

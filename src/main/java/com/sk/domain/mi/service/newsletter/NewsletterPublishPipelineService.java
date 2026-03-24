package com.sk.domain.mi.service.newsletter;

import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NewsletterPublishPipelineService {

    private final NewsletterIssueOriginalService newsletterIssueOriginalService;

    public NewsletterIssueOriginal executePublish(Integer newsletterMasterId, Integer issueNo, Integer publishHistoryId) {
        NewsletterIssueOriginal saved = newsletterIssueOriginalService
            .upsertOriginalIssue(newsletterMasterId, issueNo, publishHistoryId);
        log.info("뉴스레터 발행 스냅샷 생성 완료 - masterId: {}, issueNo: {}", newsletterMasterId, issueNo);
        return saved;
    }
}

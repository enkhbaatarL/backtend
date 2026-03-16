package com.sk.domain.mi.service.newsletter;

import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterSnapshotBackfillService {

    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterPublishHistoryRepository publishHistoryRepository;
    private final NewsletterPublishPipelineService pipelineService;

    @Transactional
    public void backfillAll(Integer batchSize, boolean dryRun) {
        int page = 0;
        Page<NewsletterIssueOriginal> slice;
        do {
            slice = issueOriginalRepository.findBySchemaVersionAndIsDeletedFalse(1, PageRequest.of(page, batchSize));
            for (NewsletterIssueOriginal issueOriginal : slice.getContent()) {
                try {
                    NewsletterPublishHistory pubHist = publishHistoryRepository.findById(issueOriginal.getSourceNewsletterPublishHistory().getNewsletterPublishId()).orElse(null);
                    if (pubHist == null) {
                        continue;
                    }
                    if (!dryRun) {
                        pipelineService.executePublish(
                            issueOriginal.getNewsletterMaster().getNewsletterMasterId(),
                            issueOriginal.getIssueNo(),
                            pubHist.getNewsletterPublishId()
                        );
                    }
                } catch (Exception e) {
                    log.error("백필 실패 - issueOriginalId: {}", issueOriginal.getNewsletterIssueOriginalId(), e);
                }
            }
            page++;
        } while (slice.hasNext());
    }
}

package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;
import com.sk.domain.shared.enums.newsletter.PublishStatusCode;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterPublishHistoryRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NewsletterPublishPipelineService {

    private final TemplateLoader templateLoader;
    private final BlockDataResolver blockDataResolver;
    private final SnapshotAssembler snapshotAssembler;
    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterPublishHistoryRepository publishHistoryRepository;

    public NewsletterIssueOriginal executePublish(Integer newsletterMasterId, Integer issueNo, Integer publishHistoryId) {
        NewsletterPublishHistory pubHist = publishHistoryRepository.findById(publishHistoryId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));

        if (!pubHist.getNewsletterMaster().getNewsletterMasterId().equals(newsletterMasterId)) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "PUBLISH_HISTORY_MASTER_MISMATCH");
        }

        TemplateTree template = templateLoader.loadByPublishHistory(pubHist);
        NewsletterIssueOriginal existingIssueOriginal = issueOriginalRepository
            .findByNewsletterMasterNewsletterMasterIdAndIssueNoAndIsDeletedFalse(newsletterMasterId, issueNo)
            .orElse(null);
        Map<String, ResolvedBlockData> resolvedData = blockDataResolver.resolveAll(template, pubHist);
        JsonNode snapshotNode = snapshotAssembler.assembleNode(template, resolvedData, pubHist);

        NewsletterIssueOriginal issueOriginal = existingIssueOriginal != null
            ? existingIssueOriginal.updateSnapshot(snapshotNode, 2, PublishStatusCode.PUBLISHED, pubHist.getPublishDateTime(), pubHist)
            : NewsletterIssueOriginal.create(
                pubHist.getNewsletterMaster(),
                pubHist,
                issueNo,
                PublishStatusCode.PUBLISHED,
                pubHist.getPublishDateTime(),
                snapshotNode,
                2
            );

        NewsletterIssueOriginal saved = issueOriginalRepository.save(issueOriginal);
        log.info("뉴스레터 발행 스냅샷 생성 완료 - masterId: {}, issueNo: {}", newsletterMasterId, issueNo);
        return saved;
    }
}

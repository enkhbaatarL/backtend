package com.sk.domain.mi.service.newsletter;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.mi.dto.response.newsletter.NewsletterIssuePreviewRes;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterSnapshotService {

    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final TemplateLoader templateLoader;
    private final BlockDataResolver blockDataResolver;
    private final SnapshotAssembler snapshotAssembler;

    public NewsletterIssuePreviewRes generateSnapshot(Integer publishHistoryId, User actorUser) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(publishHistoryId, actorUser);
        TemplateTree template = templateLoader.loadByPublishHistory(issueOriginal.getSourceNewsletterPublishHistory());
        Map<String, ResolvedBlockData> resolvedData = blockDataResolver.resolveAll(
            template,
            issueOriginal.getSourceNewsletterPublishHistory(),
            false
        );

        return new NewsletterIssuePreviewRes(
            issueOriginal.getNewsletterMaster().getNewsletterMasterId(),
            issueOriginal.getIssueNo(),
            snapshotAssembler.assembleNode(template, resolvedData, issueOriginal.getSourceNewsletterPublishHistory())
        );
    }

    private NewsletterIssueOriginal getAccessibleIssue(Integer publishHistoryId, User actorUser) {
        Integer customerCompanyNo = actorUser != null && actorUser.getCustomerCompany() != null
            ? actorUser.getCustomerCompany().getCustomerCompanyNo() : null;
        return issueOriginalRepository.findAccessibleIssueByPublishHistoryId(publishHistoryId, customerCompanyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));
    }
}

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

    public NewsletterIssuePreviewRes generateSnapshot(Integer newsletterId, Integer issueNo, User actorUser) {
        NewsletterIssueOriginal issueOriginal = getAccessibleIssue(newsletterId, issueNo, actorUser);
        TemplateTree template = templateLoader.loadByPublishHistory(issueOriginal.getSourceNewsletterPublishHistory());
        Map<String, ResolvedBlockData> resolvedData = blockDataResolver.resolveAll(
            template,
            issueOriginal.getSourceNewsletterPublishHistory(),
            false
        );

        return new NewsletterIssuePreviewRes(
            newsletterId,
            issueNo,
            snapshotAssembler.assembleNode(template, resolvedData, issueOriginal.getSourceNewsletterPublishHistory())
        );
    }

    private NewsletterIssueOriginal getAccessibleIssue(Integer newsletterId, Integer issueNo, User actorUser) {
        Integer customerCompanyNo = actorUser != null && actorUser.getCustomerCompany() != null
            ? actorUser.getCustomerCompany().getCustomerCompanyNo() : null;
        return issueOriginalRepository.findAccessibleIssue(newsletterId, issueNo, customerCompanyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));
    }
}

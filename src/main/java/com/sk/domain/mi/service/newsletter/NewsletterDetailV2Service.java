package com.sk.domain.mi.service.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.newsletter.NewsletterDetailV2Res;
import com.sk.domain.mi.dto.response.newsletter.RevisionOrOriginalDetailRes;
import com.sk.domain.mi.service.MiNewsletterRevisionService;
import com.sk.domain.shared.entity.master.tmd.NewsletterIssueOriginal;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.NewsletterIssueOriginalRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterSubscriptionRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsletterDetailV2Service {

    private final NewsletterIssueOriginalRepository issueOriginalRepository;
    private final NewsletterSubscriptionRepository newsletterSubscriptionRepository;
    private final UserRepository userRepository;
    private final MiNewsletterRevisionService revisionService;

    public NewsletterDetailV2Res getNewsletterDetail(Integer companyNo, Integer userNo, Integer newsletterId, Integer issueNo) {
        NewsletterIssueOriginal issueOriginal = issueOriginalRepository.findAccessibleIssue(newsletterId, issueNo, companyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "NEWSLETTER_NOT_FOUND"));

        JsonNode snapshot = issueOriginal.getOriginalSnapshotJson();
        String source = "ORIGINAL";

        if (issueOriginal.getSchemaVersion() != null && issueOriginal.getSchemaVersion() >= 2 && userNo != null) {
            User actorUser = userRepository.findByUserNoAndIsDeletedFalse(userNo).orElse(null);
            if (actorUser != null) {
                RevisionOrOriginalDetailRes revisionOrOriginal = revisionService.getRevisionOrOriginalDetail(newsletterId, issueNo, actorUser);
                snapshot = revisionOrOriginal.content();
                source = revisionOrOriginal.source();
            }
        }

        boolean subscribed = userNo != null && newsletterSubscriptionRepository
            .findByNewsletterMasterAndUserUserNoAndIsDeletedFalse(issueOriginal.getNewsletterMaster(), userNo)
            .map(subscription -> Boolean.TRUE.equals(subscription.getIsSubscribed()))
            .orElse(false);

        return new NewsletterDetailV2Res(newsletterId, issueNo, issueOriginal.getSchemaVersion(), source, subscribed, snapshot);
    }
}

package com.sk.domain.mi.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.newsletter.NewsletterSubscriptionRes;
import com.sk.domain.shared.entity.master.tmd.NewsletterMaster;
import com.sk.domain.shared.entity.master.tmd.NewsletterSubscription;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.NewsletterMasterRepository;
import com.sk.domain.shared.repository.master.tmd.NewsletterSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiNewsletterSubscriptionService {

    private final NewsletterMasterRepository newsletterMasterRepository;
    private final NewsletterSubscriptionRepository newsletterSubscriptionRepository;

    @Transactional
    public NewsletterSubscriptionRes subscribe(Integer newsletterId, User actorUser) {
        NewsletterMaster newsletterMaster = getAccessibleNewsletter(newsletterId, actorUser);

        NewsletterSubscription subscription = newsletterSubscriptionRepository
            .findByNewsletterMasterAndUserUserNoAndIsDeletedFalse(newsletterMaster, actorUser.getUserNo())
            .orElseGet(() -> NewsletterSubscription.subscribe(newsletterMaster, actorUser));

        subscription.subscribe();
        newsletterSubscriptionRepository.save(subscription);
        return toRes(newsletterMaster, subscription);
    }

    @Transactional
    public NewsletterSubscriptionRes unsubscribe(Integer newsletterId, User actorUser) {
        NewsletterMaster newsletterMaster = getAccessibleNewsletter(newsletterId, actorUser);

        NewsletterSubscription subscription = newsletterSubscriptionRepository
            .findByNewsletterMasterAndUserUserNoAndIsDeletedFalse(newsletterMaster, actorUser.getUserNo())
            .orElse(null);

        if (subscription != null) {
            subscription.unsubscribe();
            newsletterSubscriptionRepository.save(subscription);
        }

        return toRes(newsletterMaster, subscription);
    }

    public NewsletterSubscriptionRes getStatus(Integer newsletterId, User actorUser) {
        NewsletterMaster newsletterMaster = getAccessibleNewsletter(newsletterId, actorUser);

        NewsletterSubscription subscription = newsletterSubscriptionRepository
            .findByNewsletterMasterAndUserUserNoAndIsDeletedFalse(newsletterMaster, actorUser.getUserNo())
            .orElse(null);

        return toRes(newsletterMaster, subscription);
    }

    private NewsletterMaster getAccessibleNewsletter(Integer newsletterId, User actorUser) {
        if (actorUser == null || actorUser.getUserNo() == null || actorUser.getCustomerCompany() == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "actor user is required");
        }
        if (newsletterId == null || newsletterId <= 0) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "newsletterId is invalid");
        }

        Integer customerCompanyNo = actorUser.getCustomerCompany().getCustomerCompanyNo();

        return newsletterMasterRepository
            .findByNewsletterMasterIdAndIsDeletedFalse(newsletterId, customerCompanyNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "newsletter not found"));
    }

    private NewsletterSubscriptionRes toRes(NewsletterMaster newsletterMaster, NewsletterSubscription subscription) {
        return new NewsletterSubscriptionRes(
            newsletterMaster.getNewsletterMasterId().toString(),
            subscription != null && subscription.getIsSubscribed(),
            subscription != null ? subscription.getSubscribedAt() : null,
            subscription != null ? subscription.getUnsubscribedAt() : null
        );
    }
}

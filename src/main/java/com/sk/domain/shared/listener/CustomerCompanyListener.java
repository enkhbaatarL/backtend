package com.sk.domain.shared.listener;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCompanyListener {

    private final CacheService cacheService;

    /**
     * CustomerCompany Disabled 감지하여 CustomerCompany 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(CustomerCompany customerCompany) {

        if (!customerCompany.getIsUsed() || customerCompany.getIsDeleted()) {
            log.info("The companyNo({}) has been disabled. Subsequently, the related user cache will be invalidated", customerCompany.getCustomerCompanyNo());
            cacheService.invalidateUsersForTagEntity(CacheType.TAG_COMPANY, customerCompany.getCustomerCompanyNo());
        }

    }

}

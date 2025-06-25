package com.sk.domain.shared.listener;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostUpdate;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NoArgsConstructor
public class CustomerCompanyListener {

    public static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        CustomerCompanyListener.applicationContext = applicationContext;
    }

    /**
     * CustomerCompany(고객회사) Disabled or Deleted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(CustomerCompany customerCompany) {

        try {

            if (BooleanUtils.isFalse(customerCompany.getIsUsed()) || BooleanUtils.isTrue(customerCompany.getIsDeleted())) {
                CacheService cacheService = applicationContext.getBean(CacheService.class);
                log.info("The company({}) has been disabled or deleted. The related user's cache will be invalidated accordingly.", customerCompany.getCustomerCompanyNo());
                cacheService.invalidateUsersForTagEntity(CacheType.TAG_COMPANY, customerCompany.getCustomerCompanyNo());
            }

        } catch (Exception e) {
            log.error("Error while checking cached-state for CustomerCompany: {}", e.getMessage(), e);
        }


    }

}

package com.sk.domain.shared.listener;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.shared.entity.master.tmd.Provider;
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
public class ProviderListener {

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        ProviderListener.applicationContext = applicationContext;
    }

    /**
     * Provider(프로바이더) Disabled or Deleted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(Provider provider) {

        try {

            if (BooleanUtils.isFalse(provider.getIsUsed()) || BooleanUtils.isTrue(provider.getIsDeleted())) {
                CacheService cacheService = applicationContext.getBean(CacheService.class);
                log.info("The provider({}) has been disabled or deleted. The related user's cache will be invalidated accordingly.", provider.getProviderNo());
                cacheService.invalidateUsersForTagEntity(CacheType.TAG_PROVIDER, provider.getProviderNo());
            }

        } catch (Exception e) {
            log.error("Error while checking cached-state for Provider: {}", e.getMessage(), e);
        }

    }

}

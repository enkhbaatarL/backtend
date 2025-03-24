package com.sk.domain.shared.listener;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderListener {

    private ApplicationContext applicationContext;

    /**
     * Provider Disabled 감지하여 Provider 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(Provider provider) {

        if (!provider.getIsUsed() || provider.getIsDeleted()) {
            CacheService cacheService = applicationContext.getBean(CacheService.class);
            log.info("The providerNo({}) has been disabled. Subsequently, the related user cache will be invalidated", provider.getProviderNo());
            cacheService.invalidateUsersForTagEntity(CacheType.TAG_PROVIDER, provider.getProviderNo());
        }

    }

}

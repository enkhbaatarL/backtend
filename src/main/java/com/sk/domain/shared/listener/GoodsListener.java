package com.sk.domain.shared.listener;

import com.sk.common.config.redis.CacheType;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostUpdate;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NoArgsConstructor
public class GoodsListener {

    private ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        CustomerCompanyListener.applicationContext = applicationContext;
    }

    /**
     * Goods Disabled 감지하여 Goods 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(Goods goods) {

        if (!goods.getIsUsed() || goods.getIsDeleted()) {
            CacheService cacheService = applicationContext.getBean(CacheService.class);
            log.info("The goodsNo({}) has been disabled. Subsequently, the related user cache will be invalidated", goods.getGoodsNo());
            cacheService.invalidateUsersForTagEntity(CacheType.TAG_GOODS, goods.getGoodsNo());
        }

    }

}

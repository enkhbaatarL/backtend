package com.sk.domain.shared.listener;

import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@NoArgsConstructor
public class AuthManageGoodsMappingListener {

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        AuthManageGoodsMappingListener.applicationContext = applicationContext;
    }

    @PostUpdate
    public void checkDisabled(AuthManageGoodsMapping amgm) {

        if (BooleanUtils.isTrue(amgm.getIsDeleted()) && ObjectUtils.isNotEmpty(amgm.getUser())) {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            log.info("The AuthManageGoodsMapping(userNo: {}, companyNo: {}, goodsNo: {}, goodsAuthCd: {}) has been disabled. Subsequently, the userNo cache will be invalidated.",
                Optional.ofNullable(amgm.getUser()).map(User::getUserNo).orElse(null),
                amgm.getCustomerCompanyGoodsMapping().getCustomerCompany().getCustomerCompanyCd(),
                amgm.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo(),
                amgm.getGoodsAuthCd().getCode()
            );

            // 사용자권한-상품 매핑이 제거 되었으므로 개별 사용자의 캐싱 무효화
            cacheService.invalidateUsers(amgm.getUser().getUserNo());

        }

    }

    @PostPersist
    public void checkPersisted(AuthManageGoodsMapping amgm) {

        if (ObjectUtils.isNotEmpty(amgm.getUser())) {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            log.info("The AuthManageGoodsMapping(userNo: {}, companyNo: {}, goodsNo: {}, goodsAuthCd: {}) has been persisted. Subsequently, the userNo cache will be invalidated.",
                Optional.ofNullable(amgm.getUser()).map(User::getUserNo).orElse(null),
                amgm.getCustomerCompanyGoodsMapping().getCustomerCompany().getCustomerCompanyCd(),
                amgm.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo(),
                amgm.getGoodsAuthCd().getCode()
            );

            // 사용자권한-상품 매핑이 추가 되었으므로 개별 사용자의 캐싱 무효화
            cacheService.invalidateUsers(amgm.getUser().getUserNo());
        }

    }

}

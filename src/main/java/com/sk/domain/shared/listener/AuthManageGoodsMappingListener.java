package com.sk.domain.shared.listener;

import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd;
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

import java.util.List;
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

    /**
     * AuthManageGoodsMapping(권한-상품 매핑) Disabled or Deleted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(AuthManageGoodsMapping amgm) {

//        if (BooleanUtils.isTrue(amgm.getIsDeleted()) && ObjectUtils.isNotEmpty(amgm.getUser())) {
//
//            CacheService cacheService = applicationContext.getBean(CacheService.class);
//
//            log.info("The AuthManageGoodsMapping(userNo: {}, companyNo: {}, goodsNo: {}, goodsAuthCd: {}) has been disabled. The related user cache will be invalidated accordingly.",
//                Optional.ofNullable(amgm.getUser()).map(User::getUserNo).orElse(null),
//                amgm.getCustomerCompanyGoodsMapping().getCustomerCompany().getCustomerCompanyCd(),
//                amgm.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo(),
//                amgm.getGoodsAuthCd().getCode()
//            );
//
//            // 사용자권한-상품 매핑이 제거 되었으므로 개별 사용자의 캐싱 무효화
//            cacheService.invalidateUsers(amgm.getUser().getUserNo());
//
//        }

        if (GoodsAuthManagementSeparateCd.COMPANY == amgm.getCustomerCompanyGoodsMapping().getId().getGoodsAuthManagementSeparateCd()) {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            List<User> users = amgm.getCustomerCompanyGoodsMapping().getCustomerCompany().getUsers();
            List<Integer> userNos = users.stream().map(User::getUserNo).toList();

            log.info("The AuthManageGoodsMapping(companyNo: {}, goodsNo: {}, goodsAuthCd: {}, goodsManagementSeparatedCd: {}) has been disabled. The related user's({}) cache will be invalidated accordingly.",
                amgm.getCustomerCompanyGoodsMapping().getCustomerCompany().getCustomerCompanyNo(),
                amgm.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo(),
                amgm.getGoodsAuthCd().getCode(),
                amgm.getCustomerCompanyGoodsMapping().getId().getGoodsAuthManagementSeparateCd(),
                userNos
            );

            // 고객사-상품 매핑이 제거 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
            users.forEach(user -> cacheService.invalidateUsers(user.getUserNo()));

        }

        if (GoodsAuthManagementSeparateCd.INDIVIDUAL == amgm.getCustomerCompanyGoodsMapping().getId().getGoodsAuthManagementSeparateCd() && ObjectUtils.isNotEmpty(amgm.getUser())) {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            log.info("The AuthManageGoodsMapping(companyNo: {}, goodsNo: {}, goodsAuthCd: {}, goodsManagementSeparatedCd: {}) has been disabled. The related user's({}) cache will be invalidated accordingly.",
                amgm.getCustomerCompanyGoodsMapping().getCustomerCompany().getCustomerCompanyNo(),
                amgm.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo(),
                amgm.getGoodsAuthCd().getCode(),
                amgm.getCustomerCompanyGoodsMapping().getId().getGoodsAuthManagementSeparateCd(),
                amgm.getUser().getUserNo()
            );

            cacheService.invalidateUsers(amgm.getUser().getUserNo());

        }

    }

    /**
     * AuthManageGoodsMapping(권한-상품 매핑) Persisted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostPersist
    public void checkPersisted(AuthManageGoodsMapping amgm) {

        if (ObjectUtils.isNotEmpty(amgm.getUser())) {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            log.info("The AuthManageGoodsMapping(userNo: {}, companyNo: {}, goodsNo: {}, goodsAuthCd: {}) has been persisted. The related user cache will be invalidated accordingly.",
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

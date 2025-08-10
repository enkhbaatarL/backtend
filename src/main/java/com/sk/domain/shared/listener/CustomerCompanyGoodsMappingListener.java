package com.sk.domain.shared.listener;

import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@NoArgsConstructor
public class CustomerCompanyGoodsMappingListener {

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        CustomerCompanyGoodsMappingListener.applicationContext = applicationContext;
    }

    /**
     * CustomerCompanyGoodsMapping(고객사-상품 매핑) Disabled or Deleted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     *  - 연관된 회사의 소속 직원들의 권한 캐시를 무효화한다.
     */
    @PostUpdate
    public void checkDisabled(CustomerCompanyGoodsMapping ccgm) {

        try {

            if (BooleanUtils.isFalse(ccgm.getIsUsed()) || BooleanUtils.isTrue(ccgm.getIsDeleted())) {

                CacheService cacheService = applicationContext.getBean(CacheService.class);

                List<User> users = ccgm.getCustomerCompany().getUsers();
                List<Integer> userNos = users.stream().map(User::getUserNo).toList();

                log.info("The CustomerCompanyGoodsMapping(companyNo: {}, goodsNo: {}) has been disabled or deleted. The related user's({}) cache will be invalidated accordingly.",
                    ccgm.getCustomerCompany().getCustomerCompanyNo(),
                    ccgm.getGoods().getGoodsNo(),
                    userNos
                );

                // 고객사-상품 매핑이 제거 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
                users.forEach(user -> cacheService.invalidateUsers(user.getUserNo()));

            }

        } catch (Exception e) {
            log.error("Error while checking cached-state for CustomerCompanyGoodsMapping: {}", e.getMessage(), e);
        }

    }

    /**
     * CustomerCompanyGoodsMapping(고객사-상품 매핑) Persisted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     * - 연관된 회사의 소속 직원들의 권한 캐시를 무효화한다.
     */
    @PostPersist
    public void checkPersisted(CustomerCompanyGoodsMapping ccgm) {

        try {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            List<User> users = ccgm.getCustomerCompany().getUsers();
            List<Integer> userNos = users.stream().map(User::getUserNo).toList();

            log.info("The CustomerCompanyGoodsMapping(companyNo: {}, goodsNo: {}) has been persisted. The related user's({}) cache will be invalidated accordingly.",
                ccgm.getCustomerCompany().getCustomerCompanyNo(),
                ccgm.getGoods().getGoodsNo(),
                userNos
            );

            // 회사-상품 매핑이 추가 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
            users.forEach(user -> cacheService.invalidateUsers(user.getUserNo()));


        } catch (Exception e) {
            log.error("Error while checking cached-state for CustomerCompanyGoodsMapping: {}", e.getMessage(), e);
        }

    }

}

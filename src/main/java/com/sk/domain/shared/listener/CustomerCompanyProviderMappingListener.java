package com.sk.domain.shared.listener;

import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
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
public class CustomerCompanyProviderMappingListener {

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        CustomerCompanyProviderMappingListener.applicationContext = applicationContext;
    }

    /**
     * CustomerCompanyProviderMapping(고객사-프로바이더 매핑) Disabled or Deleted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(CustomerCompanyProviderMapping ccpm) {

        if (BooleanUtils.isFalse(ccpm.getIsUsed()) || BooleanUtils.isTrue(ccpm.getIsDeleted())) {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            List<User> users = ccpm.getCustomerCompany().getUsers();
            List<Integer> userNos = users.stream().map(User::getUserNo).toList();

            log.info("The CustomerCompanyProviderMapping(companyNo: {}, providerNo: {}) has been disabled or deleted. The related user's({}) cache will be invalidated accordingly.",
                ccpm.getCustomerCompany().getCustomerCompanyNo(),
                ccpm.getProvider().getProviderNo(),
                userNos
            );

            // 회사-프로바이더 매핑이 제거 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
            users.forEach(user -> cacheService.invalidateUsers(user.getUserNo()));

        }

    }

    /**
     * CustomerCompanyProviderMapping(고객사-프로바이더 매핑) Persisted 감지하여 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostPersist
    public void checkPersisted(CustomerCompanyProviderMapping ccpm) {

        CacheService cacheService = applicationContext.getBean(CacheService.class);

        List<User> users = ccpm.getCustomerCompany().getUsers();
        List<Integer> userNos = users.stream().map(User::getUserNo).toList();

        log.info("The CustomerCompanyProviderMapping(companyNo: {}, providerNo: {}) has been persisted. The related user's({}) cache will be invalidated accordingly.",
            ccpm.getCustomerCompany().getCustomerCompanyNo(),
            ccpm.getProvider().getProviderNo(),
            userNos
        );

        // 회사-프로바이더 매핑이 추가 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
        ccpm.getCustomerCompany().getUsers().forEach(user -> cacheService.invalidateUsers(user.getUserNo()));

    }

}

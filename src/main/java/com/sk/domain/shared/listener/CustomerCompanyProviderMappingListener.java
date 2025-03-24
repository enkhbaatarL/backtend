package com.sk.domain.shared.listener;

import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCompanyProviderMappingListener {

    private ApplicationContext applicationContext;

    /**
     * CustomerCompanyProviderMapping Disabled 감지하여 Goods 태그 엔티티 캐싱 무효화를 요청한다.
     */
    @PostUpdate
    public void checkDisabled(CustomerCompanyProviderMapping ccpm) {

        if (!ccpm.getIsUsed() || ccpm.getIsDeleted()) {

            CacheService cacheService = applicationContext.getBean(CacheService.class);

            List<User> users = ccpm.getCustomerCompany().getUsers();
            List<Integer> userNos = users.stream().map(User::getUserNo).toList();

            log.info("The CustomerCompanyProviderMapping(companyNo: {}, providerNo: {}) has been disabled. Subsequently, the userNos({}) cache will be invalidated.",
                ccpm.getCustomerCompany().getCustomerCompanyNo(), ccpm.getProvider().getProviderNo(),
                userNos);
            // 회사-프로바이더 매핑이 제거 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
            users.forEach(user -> cacheService.invalidateUsers(user.getUserNo()));
        }

    }

    @PostPersist
    public void checkPersisted(CustomerCompanyProviderMapping ccpm) {

        CacheService cacheService = applicationContext.getBean(CacheService.class);

        List<User> users = ccpm.getCustomerCompany().getUsers();
        List<Integer> userNos = users.stream().map(User::getUserNo).toList();

        log.info("The CustomerCompanyProviderMapping(companyNo: {}, providerNo: {}) has been persisted. Subsequently, the userNos({}) cache will be invalidated.",
            ccpm.getCustomerCompany().getCustomerCompanyNo(), ccpm.getProvider().getProviderNo(),
            userNos);

        // 회사-프로바이더 매핑이 추가 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
        ccpm.getCustomerCompany().getUsers().forEach(user -> cacheService.invalidateUsers(user.getUserNo()));

    }

}

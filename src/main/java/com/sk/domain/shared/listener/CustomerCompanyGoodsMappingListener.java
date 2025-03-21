package com.sk.domain.shared.listener;

import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.CacheService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerCompanyGoodsMappingListener {

    private final CacheService cacheService;

    @PostUpdate
    public void checkDisabled(CustomerCompanyGoodsMapping ccgm) {

        if (!ccgm.getIsUsed() || ccgm.getIsDeleted()) {

            List<User> users = ccgm.getCustomerCompany().getUsers();
            List<Integer> userNos = users.stream().map(User::getUserNo).toList();

            log.info("The CustomerCompanyGoodsMapping(companyNo: {}, goodsNo: {}) has been disabled. Subsequently, the userNos({}) cache will be invalidated.",
                ccgm.getCustomerCompany().getCustomerCompanyNo(), ccgm.getGoods().getGoodsNo(),
                userNos);

            // 회사-상품 매핑이 제거 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
            users.forEach(user -> cacheService.invalidateUsers(user.getUserNo()));
        }

    }

    @PostPersist
    public void checkPersisted(CustomerCompanyGoodsMapping ccgm) {

        List<User> users = ccgm.getCustomerCompany().getUsers();
        List<Integer> userNos = users.stream().map(User::getUserNo).toList();

        log.info("The CustomerCompanyGoodsMapping(companyNo: {}, goodsNo: {}) has been persisted. Subsequently, the userNos({}) cache will be invalidated.",
            ccgm.getCustomerCompany().getCustomerCompanyNo(), ccgm.getGoods().getGoodsNo(),
            userNos);

        // 회사-상품 매핑이 추가 되었으므로 해당하는 회사에 소속된 모든 사용자 캐싱 무효화
        users.forEach(user -> cacheService.invalidateUsers(user.getUserNo()));

    }

}

package com.sk.domain.shared.repository.master.tam.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QAuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QCustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tmd.QCustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QGoods;
import com.sk.domain.shared.entity.master.tmd.QProvider;
import com.sk.domain.shared.entity.master.tmd.QUser;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
public class CustomAuthManageGoodsMappingImpl implements CustomAuthManageGoodsMapping {

    private final JPAQueryFactory queryFactory;

    public List<AuthManageGoodsMapping> getUserGoodsAuthoritiesByUserLoginId(String userLoginId) {

        // 사용자T
        QUser user = QUser.user;
        // 고객회사T
        QCustomerCompany customerCompany = QCustomerCompany.customerCompany;
        // 고객회사-상품 매핑T
        QCustomerCompanyGoodsMapping ccgMapping = QCustomerCompanyGoodsMapping.customerCompanyGoodsMapping;
        // 고객회사-프로바이더 매핑T
        QCustomerCompanyProviderMapping ccpMapping = QCustomerCompanyProviderMapping.customerCompanyProviderMapping;
        // 상품T
        QGoods goods = QGoods.goods;
        // 프로바이더T
        QProvider provider = QProvider.provider;
        // 권한관리-상품 매핑T
        QAuthManageGoodsMapping authMapping = QAuthManageGoodsMapping.authManageGoodsMapping;

        // 현재 날짜 (yyyyMMdd 형식)
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return queryFactory
            .select(authMapping)
            .from(authMapping)
            .join(authMapping.user, user)
            .join(authMapping.customerCompanyGoodsMapping, ccgMapping).fetchJoin()
            .join(ccgMapping.customerCompany, customerCompany)
            .join(ccgMapping.goods, goods).fetchJoin()
            .join(goods.provider, provider)
            .join(ccpMapping).on(ccpMapping.customerCompany.eq(customerCompany).and(ccpMapping.provider.eq(provider)))
            .where(
                user.userLoginId.eq(userLoginId)
                    .and(user.isDeleted.isFalse())
                    .and(customerCompany.isDeleted.isFalse())
                    .and(ccgMapping.isDeleted.isFalse())
                    .and(ccgMapping.isUsed.isTrue())
                    .and(goods.isDeleted.isFalse())
                    .and(goods.isUsed.isTrue())
                    .and(provider.isDeleted.isFalse())
                    .and(provider.isUsed.isTrue())
                    .and(ccpMapping.isDeleted.isFalse())
                    .and(authMapping.isDeleted.isFalse())
                    .and(authMapping.useBeginDate.loe(currentDate))
                    .and(authMapping.useEndDate.goe(currentDate))
                    .and(customerCompany.useBeginDt.loe(currentDate).or(customerCompany.useBeginDt.isNull()))
                    .and(customerCompany.useEndDt.goe(currentDate).or(customerCompany.useEndDt.isNull()))
            )
            .fetch();

    }

}

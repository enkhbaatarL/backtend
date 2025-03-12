package com.sk.domain.shared.repository.master.tam.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QAuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping;
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

    public List<AuthManageGoodsMapping> getAuthority(String userLoginId) {

        QUser user = QUser.user;
        QCustomerCompany customerCompany = QCustomerCompany.customerCompany;
        QCustomerCompanyGoodsMapping ccgMapping = QCustomerCompanyGoodsMapping.customerCompanyGoodsMapping;
        QGoods goods = QGoods.goods;
        QProvider provider = QProvider.provider;
        QAuthManageGoodsMapping authMapping = QAuthManageGoodsMapping.authManageGoodsMapping;

        // 현재 날짜 (yyyyMMdd 형식)
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return queryFactory
            .select(authMapping)
            .from(user)
            .join(customerCompany).on(user.customerCompany.eq(customerCompany))
            .join(ccgMapping).on(ccgMapping.customerCompany.eq(customerCompany))
            .join(goods).on(ccgMapping.goods.eq(goods))
            .join(provider).on(goods.provider.eq(provider))
            .join(authMapping).on(
                authMapping.user.eq(user)
                    .and(authMapping.customerCompanyGoodsMapping.customerCompany.eq(customerCompany))
                    .and(authMapping.customerCompanyGoodsMapping.goods.eq(goods)))
            .where(
                user.userLoginId.eq(userLoginId)
//                    .and(ccgMapping.isUsed.isTrue())
//                    .and(goods.isUsed.isTrue())
//                    .and(provider.isUsed.isTrue())
//                    .and(authMapping.useBeginDate.loe(currentDate))
//                    .and(authMapping.useEndDate.goe(currentDate))
//                    .and(customerCompany.useBeginDt.loe(currentDate).or(customerCompany.useBeginDt.isNull()))
//                    .and(customerCompany.useEndDt.goe(currentDate).or(customerCompany.useEndDt.isNull()))
            )
            .fetch();

    }

}

package com.sk.domain.shared.repository.master.tam.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.company.GoodsWithDataCntRes;
import com.sk.domain.admin.dto.response.company.QGoodsWithDataCntRes;
import com.sk.domain.shared.entity.master.tmd.QCustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QUser;
import com.sk.domain.shared.enums.UserRoleCd;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping.customerCompanyGoodsMapping;
import static com.sk.domain.shared.entity.master.tmd.QGoodsData.goodsData;

@RequiredArgsConstructor
public class CustomCustomerCompanyGoodsMappingRepositoryImpl implements CustomCustomerCompanyGoodsMappingRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<GoodsWithDataCntRes> findGoodsMappingsByConditions(int page, int size, Integer companyNo, Integer providerNo, String search) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // User 테이블 별칭
        QUser creator = new QUser("creator");
        QUser modifier = new QUser("modifier");

        // Company 테이블 별칭
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");
        QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

        List<GoodsWithDataCntRes> content = queryFactory
            .select(new QGoodsWithDataCntRes(
                customerCompanyGoodsMapping.goods.goodsNo,
                customerCompanyGoodsMapping.goods.goodsNm,
                customerCompanyGoodsMapping.goods.goodsExpn,
                JPAExpressions.select(goodsData.count().intValue())
                    .from(goodsData)
                    .where(goodsData.isDeleted.eq(false)
                        .and(goodsData.goods.goodsNo.eq(customerCompanyGoodsMapping.goods.goodsNo))),
                customerCompanyGoodsMapping.isUsed,
                customerCompanyGoodsMapping.id.goodsAuthManagementSeparateCd,
                customerCompanyGoodsMapping.createUserId,
                customerCompanyGoodsMapping.createDt,
                customerCompanyGoodsMapping.modifyUserId,
                customerCompanyGoodsMapping.modifyDt,
                creator.userNm,
                creator.departmentNm,
                creatorCompany.customerCompanyNm,
                modifier.userNm,
                modifier.departmentNm,
                modifierCompany.customerCompanyNm
            ))
            .from(customerCompanyGoodsMapping)
            .leftJoin(creator).on(customerCompanyGoodsMapping.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.eq(UserRoleCd.ADMIN)))
            .leftJoin(creator.customerCompany, creatorCompany)
            .leftJoin(modifier).on(customerCompanyGoodsMapping.modifyUserId.eq(modifier.userLoginId).and(creator.userRoleCd.eq(UserRoleCd.ADMIN)))
            .leftJoin(modifier.customerCompany, modifierCompany)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(customerCompanyGoodsMapping.isDeleted.eq(false),
                customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo),
                searchCondition(search))
            .orderBy(customerCompanyGoodsMapping.goods.goodsNm.asc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(customerCompanyGoodsMapping.count())
            .where(customerCompanyGoodsMapping.isDeleted.eq(false),
                customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo),
                searchCondition(search));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

    private BooleanExpression searchCondition(String search) {

        if (StringUtils.isEmpty(search)) {
            return null;
        }

        return customerCompanyGoodsMapping.goods.goodsNm.containsIgnoreCase(search).or(customerCompanyGoodsMapping.goods.goodsExpn.containsIgnoreCase(search));
    }

}

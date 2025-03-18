package com.sk.domain.shared.repository.master.tam.custom;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.company.GoodsWithDataCntRes;
import com.sk.domain.admin.dto.response.company.QGoodsWithDataCntRes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping.customerCompanyGoodsMapping;
import static com.sk.domain.shared.entity.master.tmd.QGoodsDataSource.goodsDataSource;

@RequiredArgsConstructor
public class CustomCustomerCompanyGoodsMappingRepositoryImpl implements CustomCustomerCompanyGoodsMappingRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<GoodsWithDataCntRes> findGoodsMappingsByConditions(int page, int size, Integer companyNo, Integer providerNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<GoodsWithDataCntRes> content = queryFactory
            .select(new QGoodsWithDataCntRes(
                customerCompanyGoodsMapping.goods.goodsNo,
                customerCompanyGoodsMapping.goods.goodsNm,
                customerCompanyGoodsMapping.goods.goodsExpn,
                JPAExpressions.select(goodsDataSource.count().intValue())
                    .from(goodsDataSource)
                    .where(goodsDataSource.isDeleted.eq(false)
                        .and(goodsDataSource.goods.goodsNo.eq(customerCompanyGoodsMapping.goods.goodsNo))),
                customerCompanyGoodsMapping.isUsed,
                customerCompanyGoodsMapping.id.goodsAuthManageSupportCd,
                customerCompanyGoodsMapping.createUserId,
                customerCompanyGoodsMapping.createDt,
                customerCompanyGoodsMapping.modifyUserId,
                customerCompanyGoodsMapping.modifyDt
            ))
            .from(customerCompanyGoodsMapping)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(customerCompanyGoodsMapping.isDeleted.eq(false),
                customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo))
            .orderBy(customerCompanyGoodsMapping.goods.goodsNm.asc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(customerCompanyGoodsMapping.count())
            .where(customerCompanyGoodsMapping.isDeleted.eq(false),
                customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

}

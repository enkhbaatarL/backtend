package com.sk.domain.shared.repository.master.tam.custom;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.ProviderMappingGoodsRes;
import com.sk.domain.admin.dto.response.QProviderMappingGoodsRes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping.customerCompanyGoodsMapping;
import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyProviderMapping.customerCompanyProviderMapping;

@RequiredArgsConstructor
public class CustomCustomerCompanyProviderMappingRepositoryImpl implements CustomCustomerCompanyProviderMappingRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProviderMappingGoodsRes> findCompaniesMappingsByConditions(int page, int size, Integer companyNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<ProviderMappingGoodsRes> content = queryFactory
            .select(new QProviderMappingGoodsRes(
                customerCompanyProviderMapping.provider.providerNo,
                customerCompanyProviderMapping.provider.providerNm,
                JPAExpressions.select(customerCompanyGoodsMapping.count().intValue())
                    .from(customerCompanyGoodsMapping)
                    .where(customerCompanyGoodsMapping.isDeleted.eq(false)
                        .and(customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo))
                        .and(customerCompanyGoodsMapping.goods.provider.providerNo.eq(customerCompanyProviderMapping.provider.providerNo))),
                customerCompanyProviderMapping.createUserId,
                customerCompanyProviderMapping.createDt,
                customerCompanyProviderMapping.modifyUserId,
                customerCompanyProviderMapping.modifyDt
            ))
            .from(customerCompanyProviderMapping)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(customerCompanyProviderMapping.isDeleted.eq(false),
                customerCompanyProviderMapping.customerCompany.customerCompanyNo.eq(companyNo))
            .orderBy(customerCompanyProviderMapping.createDt.desc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(customerCompanyProviderMapping.count())
            .where(customerCompanyProviderMapping.isDeleted.eq(false),
                customerCompanyProviderMapping.customerCompany.customerCompanyNo.eq(companyNo));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

}

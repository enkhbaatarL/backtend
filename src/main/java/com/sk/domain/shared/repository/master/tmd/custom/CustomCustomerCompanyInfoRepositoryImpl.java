package com.sk.domain.shared.repository.master.tmd.custom;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.CustomerCompanyRes;
import com.sk.domain.admin.dto.response.QCustomerCompanyRes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;


@RequiredArgsConstructor
public class CustomCustomerCompanyInfoRepositoryImpl implements CustomCustomerCompanyInfoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CustomerCompanyRes> findCompaniesInfoByConditions(int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<CustomerCompanyRes> content = queryFactory
            .select(new QCustomerCompanyRes(
                customerCompany.customerCompanyNo,
                customerCompany.customerCompanyCd,
                customerCompany.customerCompanyNm,
                customerCompany.useBeginDt,
                customerCompany.useEndDt,
                customerCompany.createUserId,
                customerCompany.createDt,
                customerCompany.modifyUserId,
                customerCompany.modifyDt
            ))
            .from(customerCompany)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(customerCompany.isDeleted.eq(false))
            .orderBy()
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(customerCompany.count())
            .from(customerCompany)
            .where(customerCompany.isDeleted.eq(false));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

}

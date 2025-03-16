package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.CompanyRes;
import com.sk.domain.admin.dto.response.QCompanyRes;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomCustomerCompanyRepositoryImpl implements CustomCustomerCompanyRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CompanyRes> findCompaniesInfoByConditions(int page, int size, String companyNm) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<CompanyRes> content = queryFactory
                .select(new QCompanyRes(
                        customerCompany.customerCompanyNo,
                        customerCompany.customerCompanyCd,
                        customerCompany.customerCompanyNm,
                        customerCompany.useBeginDt,
                        customerCompany.useEndDt,
                        customerCompany.createUserId,
                        customerCompany.createDt,
                        customerCompany.modifyUserId,
                        customerCompany.modifyDt))
                .from(customerCompany)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .where(customerCompany.isDeleted.eq(false), containsCompanyName(companyNm))
                .orderBy(customerCompany.createDt.desc())
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory
                .select(customerCompany.count())
                .from(customerCompany)
                .where(customerCompany.isDeleted.eq(false), containsCompanyName(companyNm));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression containsCompanyName(String companyNm) {
        return StringUtils.isNotEmpty(companyNm) ? customerCompany.customerCompanyNm.contains(companyNm) : null;
    }

    @Override
    public List<CompanyDepartmentRes> findAllCompaniesAndDepartments() {
        // 1. 모든 회사 조회
        List<Tuple> companies = queryFactory
                .select(
                        customerCompany.customerCompanyNo,
                        customerCompany.customerCompanyCd,
                        customerCompany.customerCompanyNm
                )
                .from(customerCompany)
                .orderBy(customerCompany.customerCompanyNm.asc())
                .fetch();

        // 2. 회사별 부서명 조회 및 DTO 매핑
        return companies.stream().map(tuple -> {
            Integer companyNo = tuple.get(customerCompany.customerCompanyNo);
            List<String> departments = queryFactory
                    .select(user.departmentNm)
                    .from(user)
                    .where(user.customerCompany.customerCompanyNo.eq(companyNo))
                    .distinct()
                    .orderBy(user.departmentNm.asc())
                    .fetch();

            return new CompanyDepartmentRes(
                    companyNo,
                    tuple.get(customerCompany.customerCompanyCd),
                    tuple.get(customerCompany.customerCompanyNm),
                    departments.isEmpty() ? List.of() : departments // 빈 리스트 처리
            );
        }).collect(Collectors.toList());
    }

}

package com.sk.domain.shared.repository.master.tmd.custom;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.admin.dto.response.company.QCompanyRes;
import com.sk.domain.shared.entity.master.tmd.QUser;
import com.sk.domain.shared.entity.master.tmd.User;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyProviderMapping.customerCompanyProviderMapping;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;


@RequiredArgsConstructor
public class CustomCustomerCompanyRepositoryImpl implements CustomCustomerCompanyRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CompanyRes> findCompaniesByConditions(int page, int size, String companyNm) {

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
                customerCompany.modifyDt
            ))
            .from(customerCompany)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(customerCompany.isDeleted.eq(false), containsCompanyName(companyNm))
            .orderBy(customerCompany.customerCompanyNm.asc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(customerCompany.count())
            .from(customerCompany)
            .where(customerCompany.isDeleted.eq(false), containsCompanyName(companyNm));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

    @Override
    public LatestUpdatedRes findLatestUpdated(Integer companyNo) {

        // 1. 고객사 마지막 수정 일시(삭제 포함)
        Tuple company = queryFactory
            .select(customerCompany.modifyDt.max(), customerCompany.modifyUserId)
            .from(customerCompany)
            .where(customerCompany.customerCompanyNo.eq(companyNo))
            .fetchOne();

        // 2. 회사-프로바이더 매핑 마지막 수정 일시(삭제 포함)
        Tuple ccpMapping = queryFactory
            .select(customerCompanyProviderMapping.modifyDt.max(), customerCompanyProviderMapping.modifyUserId)
            .from(customerCompanyProviderMapping)
            .where(customerCompanyProviderMapping.id.customerCompanyNo.eq(companyNo))
            .fetchOne();

        // 3. 사용자 마지막 수정 일시(삭제 포함)
        Tuple userInfo = queryFactory
            .select(QUser.user.modifyDt.max(), QUser.user.modifyUserId)
            .from(QUser.user)
            .where(QUser.user.customerCompany.customerCompanyNo.eq(companyNo))
            .fetchOne();

        // 최신 날짜와 수정자 정보 찾기
        LocalDateTime latestModifyDt = null;
        String latestModifyId = null;
        String latestModifyNm = null;

        if (ObjectUtils.isNotEmpty(company)) {
            LocalDateTime date = company.get(0, LocalDateTime.class);
            latestModifyDt = date;
            latestModifyId = company.get(1, String.class);
        }

        if (ObjectUtils.isNotEmpty(ccpMapping)) {
            LocalDateTime date = ccpMapping.get(0, LocalDateTime.class);
            if (latestModifyDt == null || date.isAfter(latestModifyDt)) {
                latestModifyDt = date;
                latestModifyId = ccpMapping.get(1, String.class);
            }
        }

        if (ObjectUtils.isNotEmpty(userInfo)) {
            LocalDateTime date = userInfo.get(0, LocalDateTime.class);
            if (latestModifyDt == null || date.isAfter(latestModifyDt)) {
                latestModifyDt = date;
                latestModifyId = userInfo.get(1, String.class);
            }
        }

        if (ObjectUtils.isNotEmpty(latestModifyId)) {
            latestModifyNm = queryFactory
                .select(user.userNm)
                .from(user)
                .where(user.userLoginId.eq(latestModifyId))
                .fetchOne();
        }

        return LatestUpdatedRes.of(latestModifyId, latestModifyNm, latestModifyDt);

    }

    private BooleanExpression containsCompanyName(String companyNm) {
        return StringUtils.isNotEmpty(companyNm) ? customerCompany.customerCompanyNm.contains(companyNm) : null;
    }

}

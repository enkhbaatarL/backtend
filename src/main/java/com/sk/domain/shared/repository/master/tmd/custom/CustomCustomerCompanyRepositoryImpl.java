package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyProviderMapping.customerCompanyProviderMapping;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.response.company.CompanyDepartmentRes;
import com.sk.domain.admin.dto.response.company.CompanyRes;
import com.sk.domain.admin.dto.response.company.CompanyTypeAndCompanyRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.admin.dto.response.company.QCompanyRes;
import com.sk.domain.mi.dto.response.MiCompanyRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QCustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QUser;
import com.sk.domain.shared.enums.ActivationStatus;
import com.sk.domain.shared.enums.CompanyType;
import com.sk.domain.shared.enums.UserRoleCd;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomCustomerCompanyRepositoryImpl implements CustomCustomerCompanyRepository {
    @Value("${server.domain}")
    private String miDefaultDomain;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CompanyRes> findCompaniesByConditions(int page, int size, String companyNm, ActivationStatus activationSts) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // User 테이블 별칭
        QUser creator = new QUser("creator");
        QUser modifier = new QUser("modifier");

        // Company 테이블 별칭
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");
        QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

        List<CompanyRes> content = queryFactory
                .select(new QCompanyRes(
                        customerCompany.customerCompanyNo,
                        customerCompany.customerCompanyCd,
                        customerCompany.customerCompanyNm,
                        customerCompany.useBeginDt,
                        customerCompany.useEndDt,
                        customerCompany.isUsed,
                        customerCompany.createUserId,
                        customerCompany.createDt,
                        customerCompany.modifyUserId,
                        customerCompany.modifyDt,
                        creator.userNm,
                        creator.departmentNm,
                        creatorCompany.customerCompanyNm,
                        modifier.userNm,
                        modifier.departmentNm,
                        modifierCompany.customerCompanyNm))
                .from(customerCompany)
                .leftJoin(creator).on(customerCompany.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.eq(UserRoleCd.ADMIN)))
                .leftJoin(creator.customerCompany, creatorCompany)
                .leftJoin(modifier).on(customerCompany.modifyUserId.eq(modifier.userLoginId).and(modifier.userRoleCd.eq(UserRoleCd.ADMIN)))
                .leftJoin(modifier.customerCompany, modifierCompany)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .where(customerCompany.isDeleted.isFalse().and(customerCompany.isMipManagementCompany.isFalse()), // edit(20250411): 고객사만 조회
                        containsCompanyName(companyNm),
                        equalIsUsed(activationSts))
                .orderBy(customerCompany.customerCompanyNm.asc())
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory
                .select(customerCompany.count())
                .from(customerCompany)
                .where(customerCompany.isDeleted.isFalse().and(customerCompany.isMipManagementCompany.isFalse()), // edit(20250411): 고객사만 조회
                        containsCompanyName(companyNm),
                        equalIsUsed(activationSts));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<CompanyRes> findOwnerCompaniesByConditions(int page, int size, String companyNm) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // User 테이블 별칭
        QUser creator = new QUser("creator");
        QUser modifier = new QUser("modifier");

        // Company 테이블 별칭
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");
        QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

        List<CompanyRes> content = queryFactory
                .select(new QCompanyRes(
                        customerCompany.customerCompanyNo,
                        customerCompany.customerCompanyCd,
                        customerCompany.customerCompanyNm,
                        customerCompany.useBeginDt,
                        customerCompany.useEndDt,
                        customerCompany.isUsed,
                        customerCompany.createUserId,
                        customerCompany.createDt,
                        customerCompany.modifyUserId,
                        customerCompany.modifyDt,
                        creator.userNm,
                        creator.departmentNm,
                        creatorCompany.customerCompanyNm,
                        modifier.userNm,
                        modifier.departmentNm,
                        modifierCompany.customerCompanyNm))
                .from(customerCompany)
                .leftJoin(creator).on(customerCompany.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.eq(UserRoleCd.ADMIN)))
                .leftJoin(creator.customerCompany, creatorCompany)
                .leftJoin(modifier).on(customerCompany.modifyUserId.eq(modifier.userLoginId).and(modifier.userRoleCd.eq(UserRoleCd.ADMIN)))
                .leftJoin(modifier.customerCompany, modifierCompany)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .where(customerCompany.isDeleted.isFalse().and(customerCompany.isMipManagementCompany.isTrue()), // edit(20250411): 관리사만 조회
                        containsCompanyName(companyNm))
                .orderBy(customerCompany.customerCompanyNm.asc())
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory
                .select(customerCompany.count())
                .from(customerCompany)
                .where(customerCompany.isDeleted.isFalse().and(customerCompany.isMipManagementCompany.isTrue()), // edit(20250411): 관리사만 조회
                        containsCompanyName(companyNm));

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

        if (ObjectUtils.isEmpty(company.get(0, LocalDateTime.class))) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company]. companyNo: %s".formatted(companyNo));
        }

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
            if (latestModifyDt == null || date != null && date.isAfter(latestModifyDt)) {
                latestModifyDt = date;
                latestModifyId = ccpMapping.get(1, String.class);
            }
        }

        if (ObjectUtils.isNotEmpty(userInfo)) {
            LocalDateTime date = userInfo.get(0, LocalDateTime.class);
            if (latestModifyDt == null || date != null && date.isAfter(latestModifyDt)) {
                latestModifyDt = date;
                latestModifyId = userInfo.get(1, String.class);
            }
        }

        if (ObjectUtils.isNotEmpty(latestModifyId)) {
            latestModifyNm = queryFactory
                    .select(user.userNm)
                    .from(user)
                    .where(user.userLoginId.eq(latestModifyId).and(user.userRoleCd.eq(UserRoleCd.ADMIN)))
                    .limit(1)
                    .fetchOne();
        }

        return LatestUpdatedRes.of(latestModifyId, latestModifyNm, latestModifyDt);

    }

    private BooleanExpression containsCompanyName(String companyNm) {
        return StringUtils.isNotEmpty(companyNm) ? customerCompany.customerCompanyNm.contains(companyNm) : null;
    }

    private BooleanExpression equalIsUsed(ActivationStatus activationSts) {
        return ObjectUtils.isNotEmpty(activationSts) ? customerCompany.isUsed.eq(activationSts.isActive()) : null;
    }

    /**
     * 현재 정책 : 고객사 한정
     */
    @Override
    public List<CompanyDepartmentRes> findAllCompaniesAndDepartments() {
        // 1. 모든 회사 조회
        List<Tuple> companies = queryFactory
                .select(customerCompany.customerCompanyNo, customerCompany.customerCompanyCd, customerCompany.customerCompanyNm)
                .from(customerCompany)
                .where(customerCompany.isDeleted.isFalse().and(customerCompany.isMipManagementCompany.isFalse()))
                .orderBy(customerCompany.customerCompanyNm.asc())
                .fetch();

        // 2. 모든 회사의 부서 조회를 한 번에 수행
        List<Integer> companyNos = companies.stream()
                .map(tuple -> tuple.get(customerCompany.customerCompanyNo))
                .collect(Collectors.toList());

        Map<Integer, List<String>> departmentMap = queryFactory
                .select(user.customerCompany.customerCompanyNo, user.departmentNm)
                .from(user)
                .where(user.customerCompany.customerCompanyNo.in(companyNos))
                .distinct()
                .orderBy(user.departmentNm.asc())
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(user.customerCompany.customerCompanyNo),
                        Collectors.mapping(tuple -> tuple.get(user.departmentNm), Collectors.toList())));

        // 3. DTO 매핑
        return companies.stream().map(tuple -> {
            Integer companyNo = tuple.get(customerCompany.customerCompanyNo);
            List<String> departments = departmentMap.getOrDefault(companyNo, List.of());
            return new CompanyDepartmentRes(
                    companyNo,
                    tuple.get(customerCompany.customerCompanyCd),
                    tuple.get(customerCompany.customerCompanyNm),
                    departments);
        }).collect(Collectors.toList());
    }

    @Override
    public List<CompanyTypeAndCompanyRes> findCompaniesByTypeWithSubCompanies() {
        List<CustomerCompany> companies = queryFactory
                .selectFrom(customerCompany)
                .where(customerCompany.isUsed.isTrue(), customerCompany.isDeleted.isFalse())
                .orderBy(customerCompany.customerCompanyNo.asc())
                .fetch();

        Map<CompanyType, List<CompanyTypeAndCompanyRes.Company>> grouped = companies.stream()
                .collect(Collectors.groupingBy(
                        company -> company.getIsMipManagementCompany() ? CompanyType.MANAGER : CompanyType.CUSTOMER,
                        Collectors.mapping(
                                company -> new CompanyTypeAndCompanyRes.Company(
                                        company.getCustomerCompanyNo(),
                                        company.getCustomerCompanyCd(),
                                        company.getCustomerCompanyNm()),
                                Collectors.toList())));

        return Arrays.stream(CompanyType.values())
                .map(type -> new CompanyTypeAndCompanyRes(type, grouped.getOrDefault(type, Collections.emptyList())))
                .filter(res -> !res.companies().isEmpty())
                .toList();
    }

    @Override
    public List<MiCompanyRes> findAllMiCompanies() {
        List<CustomerCompany> companies = queryFactory
                .selectFrom(customerCompany)
                .where(customerCompany.isUsed.isTrue(), customerCompany.isDeleted.isFalse(), customerCompany.isMipManagementCompany.isFalse(),
                        customerCompany.useBeginDt.loe(DateUtil.toYmd(LocalDate.now()))
                                .and(customerCompany.useEndDt.goe(DateUtil.toYmd(LocalDate.now()))))
                .orderBy(customerCompany.customerCompanyNo.asc())
                .fetch();

        return companies.stream()
                .map(customerCompany -> MiCompanyRes.of(customerCompany, miDefaultDomain))
                .collect(Collectors.toList());
    }
}
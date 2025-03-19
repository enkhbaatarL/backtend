package com.sk.domain.shared.repository.master.tam.custom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.response.company.AuthGoodsMapRes;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QAuthManageGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tam.QCustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tmd.*;
import com.sk.domain.shared.enums.GoodsAuthCd;
import com.sk.domain.shared.enums.GoodsAuthManageSupportCd;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sk.domain.shared.entity.master.tam.QAuthManageGoodsMapping.authManageGoodsMapping;
import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping.customerCompanyGoodsMapping;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

@RequiredArgsConstructor
public class CustomAuthManageGoodsMappingImpl implements CustomAuthManageGoodsMapping {

    // 사용자T
    QUser user = QUser.user;
    // 고객회사T
    QCustomerCompany customerCompany = QCustomerCompany.customerCompany;
    // 프로바이더T
    QProvider provider = QProvider.provider;
    // 상품T
    QGoods goods = QGoods.goods;
    // 고객회사-프로바이더 매핑T
    QCustomerCompanyProviderMapping ccpMapping = QCustomerCompanyProviderMapping.customerCompanyProviderMapping;
    // 고객회사-상품 매핑T
    QCustomerCompanyGoodsMapping ccgMapping = customerCompanyGoodsMapping;
    // 권한-상품 매핑T
    QAuthManageGoodsMapping authMapping = authManageGoodsMapping;

    private final JPAQueryFactory queryFactory;

    public List<AuthManageGoodsMapping> findUserGoodsPermissions(Integer userNo) {

        String currentDate = DateUtil.toYmd(LocalDate.now());

        return queryFactory
            .select(authMapping)
            .from(authMapping)
            .join(authMapping.user, user).fetchJoin()  // fetchJoin 추가
            .join(authMapping.customerCompanyGoodsMapping, ccgMapping).fetchJoin()
            .join(ccgMapping.customerCompany, customerCompany).fetchJoin()  // fetchJoin 추가
            .join(ccgMapping.goods, goods).fetchJoin()
            .join(goods.provider, provider).fetchJoin()  // fetchJoin 추가
            .join(ccpMapping).on(ccpMapping.customerCompany.eq(customerCompany).and(ccpMapping.provider.eq(provider)))
            .where(
                user.userNo.eq(userNo)
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

    public List<AuthManageGoodsMapping> findByCompanyNoAndGoodsNoAndGoodsAuthCd(Integer companyNo, Integer goodsNo, GoodsAuthManageSupportCd goodsAuthManageSupportCd) {

        return queryFactory
            .selectFrom(authMapping)
            .where(authMapping.isDeleted.isFalse()
                .and(authMapping.customerCompanyGoodsMapping.id.customerCompanyNo.eq(companyNo))
                .and(authMapping.customerCompanyGoodsMapping.id.goodsNo.eq(goodsNo))
                .and(authMapping.customerCompanyGoodsMapping.id.goodsAuthManageSupportCd.eq(goodsAuthManageSupportCd)))
            .fetch();
    }

    @Override
    public Page<AuthGoodsMapRes> findByAuthGoodsByConditions(int page, int size, Integer companyNo, Integer goodsNo) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // todo 회원명 검색 / 회원수, 조회수, 다운로드수

        // 1. 고객사의 사용자 목록을 페이지네이션으로 조회
        List<User> users = queryFactory
            .selectFrom(user)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(user.isDeleted.isFalse(),
                user.customerCompany.customerCompanyNo.eq(companyNo))
            .orderBy(user.userNm.asc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(user.count())
            .from(user)
            .where(user.isDeleted.isFalse(),
                user.customerCompany.customerCompanyNo.eq(companyNo));

        // 2. 해당 상품에 대한 권한 매핑 정보를 모두 조회
        List<AuthManageGoodsMapping> authGoodsMappings = queryFactory
            .select(authManageGoodsMapping)
            .from(authManageGoodsMapping)
            .leftJoin(authManageGoodsMapping.user, user).fetchJoin()
            .innerJoin(authManageGoodsMapping.customerCompanyGoodsMapping, customerCompanyGoodsMapping).fetchJoin()
            .innerJoin(customerCompanyGoodsMapping.customerCompany, customerCompany).fetchJoin()
            .where(authManageGoodsMapping.isDeleted.isFalse(),
                customerCompanyGoodsMapping.id.goodsNo.eq(goodsNo), customerCompany.customerCompanyNo.eq(companyNo))
            .fetch();

        GoodsAuthManageSupportCd authSupportCd = authGoodsMappings.get(0).getCustomerCompanyGoodsMapping().getId().getGoodsAuthManageSupportCd();

        Map<Integer, Boolean[]> individualAuthMap = new HashMap<>();
        Boolean[] companyAuth = new Boolean[]{Boolean.FALSE, Boolean.FALSE};

        for (AuthManageGoodsMapping mapping : authGoodsMappings) {

            if (mapping.getCustomerCompanyGoodsMapping().getId().getGoodsAuthManageSupportCd() == GoodsAuthManageSupportCd.INDIVIDUAL) {

                Integer userNo = mapping.getUser().getUserNo();
                // 해당 사용자의 권한 배열 가져오기 (없으면 [false, false] 생성)
                Boolean[] goodsAuthBool = individualAuthMap.computeIfAbsent(userNo, k -> new Boolean[]{Boolean.FALSE, Boolean.FALSE});

                // 권한 유형에 따라 해당 배열 요소 업데이트
                if (mapping.getGoodsAuthCd() == GoodsAuthCd.VIEW) {
                    goodsAuthBool[0] = Boolean.TRUE;
                } else if (mapping.getGoodsAuthCd() == GoodsAuthCd.DOWNLOAD) {
                    goodsAuthBool[1] = Boolean.TRUE;
                }

            } else {

                if (mapping.getGoodsAuthCd() == GoodsAuthCd.VIEW) {
                    companyAuth[0] = Boolean.TRUE;
                } else if (mapping.getGoodsAuthCd() == GoodsAuthCd.DOWNLOAD) {
                    companyAuth[1] = Boolean.TRUE;
                }

            }

        }

        List<AuthGoodsMapRes> result = users.stream().map(user -> {

            if (authSupportCd == GoodsAuthManageSupportCd.COMPANY) {
                return AuthGoodsMapRes.of(user, companyAuth);
            } else {
                Boolean[] authArrays = individualAuthMap.getOrDefault(user.getUserNo(), new Boolean[]{Boolean.FALSE, Boolean.FALSE});
                return AuthGoodsMapRes.of(user, authArrays);
            }

        }).toList();

        return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);
    }

    @Override
    public LatestUpdatedRes findLatestUpdated(Integer companyNo, Integer providerNo, Integer goodsNo) {

        // 1. 고객사-상품 매핑 수정 일시(삭제 포함)
        Tuple ccgMappings = queryFactory
            .select(customerCompanyGoodsMapping.modifyDt.max(), customerCompanyGoodsMapping.modifyUserId)
            .from(customerCompanyGoodsMapping)
            .where(customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo)
                .and(customerCompanyGoodsMapping.goods.goodsNo.eq(goodsNo)))
            .fetchOne();

        if (ObjectUtils.isEmpty(ccgMappings.get(0, LocalDateTime.class))) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company-goods-mapping]. companyNo: %s, goodsNo: %s".formatted(companyNo, goodsNo));
        }

        // 2. 사용자 권한-상품 매핑 수정 일시(삭제 포함)
        Tuple amgMappings = queryFactory
            .select(authManageGoodsMapping.modifyDt.max(), authManageGoodsMapping.modifyUserId)
            .from(authManageGoodsMapping)
            .where(authManageGoodsMapping.customerCompanyGoodsMapping.id.customerCompanyNo.eq(companyNo)
                .and(authManageGoodsMapping.customerCompanyGoodsMapping.id.goodsNo.eq(goodsNo)))
            .fetchOne();

        // 최신 날짜와 수정자 정보 찾기
        LocalDateTime latestModifyDt = null;
        String latestModifyId = null;
        String latestModifyNm = null;

        if (ObjectUtils.isNotEmpty(ccgMappings)) {
            LocalDateTime date = ccgMappings.get(0, LocalDateTime.class);
            latestModifyDt = date;
            latestModifyId = ccgMappings.get(1, String.class);
        }

        if (ObjectUtils.isNotEmpty(amgMappings)) {
            LocalDateTime date = amgMappings.get(0, LocalDateTime.class);
            if (latestModifyDt == null || date.isAfter(latestModifyDt)) {
                latestModifyDt = date;
                latestModifyId = amgMappings.get(1, String.class);
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

}

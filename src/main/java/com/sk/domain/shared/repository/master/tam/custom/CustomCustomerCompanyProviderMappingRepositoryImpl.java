package com.sk.domain.shared.repository.master.tam.custom;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.admin.dto.response.company.LatestUpdatedRes;
import com.sk.domain.admin.dto.response.company.ProviderWithGoodsCntRes;
import com.sk.domain.admin.dto.response.company.QProviderWithGoodsCntRes;
import com.sk.domain.shared.entity.master.tmd.QCustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QUser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyGoodsMapping.customerCompanyGoodsMapping;
import static com.sk.domain.shared.entity.master.tam.QCustomerCompanyProviderMapping.customerCompanyProviderMapping;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

@RequiredArgsConstructor

public class CustomCustomerCompanyProviderMappingRepositoryImpl implements CustomCustomerCompanyProviderMappingRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ProviderWithGoodsCntRes> findCompanyMappingsWithGoodsCntByCompanyNo(Integer companyNo) {

        // User 테이블 별칭
        QUser creator = new QUser("creator");
        QUser modifier = new QUser("modifier");

        // Company 테이블 별칭
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");
        QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

        return queryFactory
            .select(new QProviderWithGoodsCntRes(
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
                customerCompanyProviderMapping.modifyDt,
                creator.userNm,
                creator.departmentNm,
                creatorCompany.customerCompanyNm,
                modifier.userNm,
                modifier.departmentNm,
                modifierCompany.customerCompanyNm
            ))
            .from(customerCompanyProviderMapping)
            .leftJoin(creator).on(customerCompany.createUserId.eq(creator.userLoginId))
            .leftJoin(creator.customerCompany, creatorCompany)
            .leftJoin(modifier).on(customerCompany.modifyUserId.eq(modifier.userLoginId))
            .leftJoin(modifier.customerCompany, modifierCompany)
            .where(customerCompanyProviderMapping.isDeleted.eq(false),
                customerCompanyProviderMapping.customerCompany.customerCompanyNo.eq(companyNo))
            .orderBy(customerCompanyProviderMapping.provider.providerNm.asc())
            .fetch();
    }

    @Override
    public LatestUpdatedRes findLatestUpdated(Integer companyNo, Integer providerNo) {

        // 1. 고객사-프로바이더 매핑 수정 일시(삭제 포함)
        Tuple ccpMappings = queryFactory
            .select(customerCompanyProviderMapping.modifyDt.max(), customerCompanyProviderMapping.modifyUserId)
            .from(customerCompanyProviderMapping)
            .where(customerCompanyProviderMapping.customerCompany.customerCompanyNo.eq(companyNo)
                .and(customerCompanyProviderMapping.provider.providerNo.eq(providerNo)))
            .fetchOne();

        if (ObjectUtils.isEmpty(ccpMappings.get(0, LocalDateTime.class))) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [company-provider-mapping]. companyNo: %s, providerNo: %s".formatted(companyNo, providerNo));
        }

        // 2. 고객사-상품 매핑 수정 일시(삭제 포함)
        Tuple ccgMappings = queryFactory
            .select(customerCompanyGoodsMapping.modifyDt.max(), customerCompanyGoodsMapping.modifyUserId)
            .from(customerCompanyGoodsMapping)
            .where(customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo))
            .fetchOne();

        // 최신 날짜와 수정자 정보 찾기
        LocalDateTime latestModifyDt = null;
        String latestModifyId = null;
        String latestModifyNm = null;

        if (ObjectUtils.isNotEmpty(ccpMappings)) {
            LocalDateTime date = ccpMappings.get(0, LocalDateTime.class);
            latestModifyDt = date;
            latestModifyId = ccpMappings.get(1, String.class);
        }

        if (ObjectUtils.isNotEmpty(ccgMappings)) {
            LocalDateTime date = ccgMappings.get(0, LocalDateTime.class);
            if (latestModifyDt == null || date.isAfter(latestModifyDt)) {
                latestModifyDt = date;
                latestModifyId = ccgMappings.get(1, String.class);
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

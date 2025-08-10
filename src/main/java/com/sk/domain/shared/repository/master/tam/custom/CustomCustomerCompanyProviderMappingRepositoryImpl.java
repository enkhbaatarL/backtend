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
import com.sk.domain.shared.enums.UserRoleCd;
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
            .leftJoin(creator).on(customerCompanyProviderMapping.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.eq(UserRoleCd.ADMIN).and(creator.isDeleted.isFalse())))
            .leftJoin(creator.customerCompany, creatorCompany)
            .leftJoin(modifier).on(customerCompanyProviderMapping.modifyUserId.eq(modifier.userLoginId).and(modifier.userRoleCd.eq(UserRoleCd.ADMIN).and(modifier.isDeleted.isFalse())))
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
            .select(customerCompanyProviderMapping.modifyDt, customerCompanyProviderMapping.modifyUserId)
            .from(customerCompanyProviderMapping)
            .where(customerCompanyProviderMapping.customerCompany.customerCompanyNo.eq(companyNo)
                .and(customerCompanyProviderMapping.provider.providerNo.eq(providerNo)))
            .orderBy(customerCompanyProviderMapping.modifyDt.desc()) // 최신순 정렬
            .limit(1) // 가장 최근 1건
            .fetchOne();

        if (ccpMappings == null || ccpMappings.get(customerCompanyProviderMapping.modifyDt) == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [company-provider-mapping] or has no modification date. companyNo: %s, providerNo: %s".formatted(companyNo, providerNo));
        }

        // 2. 고객사-상품 매핑 수정 일시(삭제 포함)
        Tuple ccgMappings = queryFactory
            .select(customerCompanyGoodsMapping.modifyDt, customerCompanyGoodsMapping.modifyUserId)
            .from(customerCompanyGoodsMapping)
            .where(customerCompanyGoodsMapping.customerCompany.customerCompanyNo.eq(companyNo))
            .orderBy(customerCompanyGoodsMapping.modifyDt.desc()) // 최신순 정렬
            .limit(1) // 가장 최근 1건
            .fetchOne();

        // 최신 날짜와 수정자 정보 찾기
        LocalDateTime latestModifyDt;
        String latestModifyId;
        String latestModifyNm = null;

        latestModifyDt = ccpMappings.get(customerCompanyProviderMapping.modifyDt);
        latestModifyId = ccpMappings.get(customerCompanyProviderMapping.modifyUserId);

        if (ccgMappings != null) {
            LocalDateTime date = ccgMappings.get(customerCompanyGoodsMapping.modifyDt);
            String userId = ccgMappings.get(customerCompanyGoodsMapping.modifyUserId);

            if (date != null && (latestModifyDt == null || date.isAfter(latestModifyDt))) {
                latestModifyDt = date;
                latestModifyId = userId;
            }
        }

        if (latestModifyId != null && !latestModifyId.isEmpty()) {
            latestModifyNm = queryFactory
                .select(user.userNm)
                .from(user)
                .where(user.userLoginId.eq(latestModifyId).and(user.userRoleCd.eq(UserRoleCd.ADMIN)))
                .limit(1)
                .fetchOne();
        }

        return LatestUpdatedRes.of(latestModifyId, latestModifyNm, latestModifyDt);

    }

}

package com.sk.domain.shared.repository.master.tlm;

import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInquiryUserId;
import com.sk.domain.shared.entity.master.tlm.GoodsDataInquiryUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoodsDataInquiryUserRepository extends JpaRepository<GoodsDataInquiryUser, GoodsDataInquiryUserId> {

    @Query(value = """
                SELECT new com.sk.domain.mi.dto.response.marketData.RecentViewedRes(
                    p.providerNo,
                    p.providerNm,
                    g.goodsNo,
                    gd.goodsDataNo,
                    gd.goodsDataNm,
                    gd.goodsDataExpn,
                    MAX(gdiu.id.inquiryDtm)
                )
                FROM GoodsDataInquiryUser gdiu
                JOIN gdiu.id.goodsData gd
                JOIN gd.goods g
                JOIN g.provider p
                WHERE
                    gdiu.id.userLoginId = :userLoginId
                    AND EXISTS (
                            SELECT 1
                            FROM CustomerCompanyProviderMapping ccpm
                                JOIN ccpm.customerCompany cc
                                JOIN CustomerCompanyGoodsMapping ccgm ON ccgm.customerCompany = cc
                                JOIN AuthManageGoodsMapping amgm ON amgm.customerCompanyGoodsMapping.customerCompany = cc
                            WHERE
                                ccpm.isDeleted = false
                                AND ccpm.isUsed = true
                                AND ccgm.isDeleted = false
                                AND ccgm.isUsed = true
                                AND amgm.isDeleted = false
                                AND ccgm.id.goodsAuthManagementSeparateCd = amgm.customerCompanyGoodsMapping.id.goodsAuthManagementSeparateCd
                                AND ccgm.goods.goodsNo = g.goodsNo
                                AND cc.customerCompanyNo = :companyNo
                                AND amgm.useBeginDate <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                                AND amgm.useEndDate >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                                AND amgm.goodsAuthCd = 'VIEW'
                                AND (ccgm.id.goodsAuthManagementSeparateCd = 'COMPANY' OR amgm.user.userNo = :userNo)
                    )
                    AND EXISTS (
                            SELECT 1
                            FROM GoodsDataSourceDetail gdsd
                            WHERE gdsd.goodsData = gd
                                AND gdsd.isDeleted = false
                                AND gdsd.goodsDataUseManagementCd = 'backend'
                    )
                GROUP BY
                    p.providerNo,
                    p.providerNm,
                    g.goodsNo,
                    gd.goodsDataNo,
                    gd.goodsDataNm,
                    gd.goodsDataExpn
                ORDER BY
                    MAX(gdiu.id.inquiryDtm) DESC
            """)
    List<RecentViewedRes> findRecentViewdByUserLoginId(@Param("companyNo") int companyNo, @Param("userNo") int userNo,
            @Param("userLoginId") String userLoginId, Pageable pageable);
}

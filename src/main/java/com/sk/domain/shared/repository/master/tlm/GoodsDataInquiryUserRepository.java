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
                    g.goodsNm,
                    gd.goodsDataNo,
                    gd.goodsDataNm,
                    gd.goodsDataExpn,
                    gd.modifyDt
                ),
                FUNCTION('MAX',gdiu.createDt) as createDt
                FROM GoodsDataInquiryUser gdiu
                LEFT JOIN GoodsData gd ON gd.goodsDataNo = gdiu.id.goodsData.goodsDataNo
                LEFT JOIN Goods g ON g = gdiu.id.goodsData.goods
                LEFT JOIN Provider p ON p.providerNo = g.provider.providerNo
                JOIN (
                    SELECT DISTINCT gdsd.goodsData.goodsDataNo as goodsDataNo
                    FROM GoodsDataSourceDetail gdsd
                    WHERE gdsd.isDeleted = false
                ) gud ON gud.goodsDataNo = gd.goodsDataNo
                WHERE  gdiu.id.userLoginId = :userLoginId
                    AND gdiu.isDeleted = false
                    AND p.isDeleted = false
                    AND p.isUsed = true
                    AND g.isDeleted = false
                    AND g.isUsed = true
                    AND gd.isDeleted = false
                GROUP BY p.providerNo, g.goodsNo, gd.goodsDataNo
                ORDER BY gdiu.createDt DESC
            """, countQuery = """

            """)
    List<RecentViewedRes> findById_UserLoginIdAndIsDeletedFalse(@Param("userLoginId") String userLoginId, Pageable pageable);
}

package com.sk.domain.shared.repository.master.tmd;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.*;
import com.sk.domain.mi.dto.response.dashboard.LatestDataRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.mi.dto.response.insights.FocusedDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

public interface GoodsDataRepository extends JpaRepository<GoodsData, Integer> {
    /**
     *
     * 기본적으로 고객사나 사용자의 사용가능 상태는 체크 하지 않는다. 로그인 단에서 체크하는걸로 처리
     *
     */

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataRes(
                cc.customerCompanyNm,
                gd.goodsDataNo,
                gd.goodsDataNm,
                gd.goodsDataExpn
            )
            FROM GoodsData gd
            LEFT JOIN CustomerCompany cc ON gd.customerCompany = cc
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
                AND (:keyword IS NULL OR (LOWER(gd.goodsDataNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(gd.goodsDataExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            ORDER BY gd.goodsDataNm ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT gd.goodsDataNo)
            FROM GoodsData gd
            LEFT JOIN CustomerCompany cc ON gd.customerCompany = cc
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
                AND (:keyword IS NULL OR (LOWER(gd.goodsDataNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(gd.goodsDataExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            """)
    Page<GoodsDataRes> findGoodsDataByProviderNoAndGoodsNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo,
            @Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataNameRes(
            gd.goodsDataNo,
            gd.goodsDataNm
            )
            FROM GoodsData gd
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
            ORDER BY gd.goodsDataNm ASC
            """, countQuery = """
            SELECT COUNT(gd.goodsDataNo)
            FROM GoodsData gd
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
                        """)
    List<GoodsDataNameRes> findGoodsDataNamesByProviderNoAndGoodsNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo);

    @Query(value = """
            SELECT gd
            FROM GoodsData gd
            WHERE gd.isDeleted=false
                AND gd.goods.provider.providerNo=:providerNo
                AND gd.goods.provider.isDeleted=false
                AND gd.goods.goodsNo=:goodsNo
                AND gd.goods.isDeleted=false
                AND gd.goodsDataNo=:goodsDataNo
                """)
    Optional<GoodsData> findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo,
            @Param("goodsDataNo") int goodsDataNo);

    @EntityGraph(attributePaths = {"goods", "goods.provider"})
    Optional<GoodsData> findByGoodsDataNoAndIsDeletedFalse(@Param("goodsDataNo") int goodsDataNo);

    //
    @Query(value = """
            SELECT
                p.providerNo as providerNo,
                p.providerNm as providerNm,
                g.goodsNo as goodsNo,
                g.goodsNm as goodsNm,
                gd.goodsDataNo as goodsDataNo,
                gd.goodsDataNm as goodsDataNm,
                gd.modifyDt as modifyDt,
                FUNCTION('GROUP_CONCAT', gdc.goodsDataCategoryCd) as goodsDataCategoryCd
            FROM CustomerCompanyGoodsMapping ccgm
            LEFT JOIN Goods g ON g.goodsNo = ccgm.id.goodsNo
            LEFT JOIN Provider p ON p.providerNo = g.provider.providerNo
            LEFT JOIN GoodsData gd ON gd.goods.goodsNo = g.goodsNo
            LEFT JOIN GoodsDataCategory gdc ON gdc.goodsData.goodsDataNo = gd.goodsDataNo
            LEFT JOIN CommonCodeDetail ccd ON ccd.id.codeId = gdc.goodsDataCategoryCd
            JOIN (
                SELECT DISTINCT gdc.goodsData.goodsDataNo as goodsDataNo
                FROM GoodsDataCategory gdc
                WHERE gdc.isDeleted = false
                AND gdc.goodsDataManagementSeparateCd = 'GUDAT'
            ) gud ON gud.goodsDataNo = gd.goodsDataNo
            WHERE ccgm.id.customerCompanyNo                = :customerCompanyNo
                AND ccgm.isDeleted                           = false
                AND ccgm.isUsed                              = true
                AND g.isDeleted                              = false
                AND g.isUsed                                 = true
                AND p.isDeleted                              = false
                AND p.isUsed                                 = true
                AND gd.isDeleted                             = false
                AND gd.isGoodsDataMarked                     = true
                AND gdc.isDeleted                            = false
                AND ccd.highCodeId                            = 'A_TYPES'
            GROUP BY p.providerNo, p.providerNm, g.goodsNo, g.goodsNm, gd.goodsDataNo, gd.goodsDataNm
            ORDER BY gd.goodsDataNo DESC
            """)
    List<FocusedDataRes> findFocusedData(@Param("customerCompanyNo") int customerCompanyNo, Pageable pageable);

    Optional<GoodsData> findByGoodsDataNoAndGoods_GoodsNoAndIsDeletedFalse(Integer goodsDataNo, Integer goodsNo);

    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.dashboard.LatestDataRes(
                p.providerNo,
                p.providerNm,
                g.goodsNo,
                g.goodsNm,
                gd.goodsDataNo,
                gd.goodsDataNm,
                gd.modifyDt
            )
            FROM CustomerCompanyGoodsMapping ccgm
            LEFT JOIN Goods g ON g.goodsNo = ccgm.id.goodsNo
            LEFT JOIN Provider p ON p.providerNo = g.provider.providerNo
            LEFT JOIN GoodsData gd ON gd.goods.goodsNo = g.goodsNo
            WHERE ccgm.id.customerCompanyNo = :customerCompanyNo
                AND ccgm.isDeleted = false
                AND ccgm.isUsed = true
                AND g.isDeleted = false
                AND g.isUsed = true
                AND p.isDeleted = false
                AND p.isUsed = true
                AND gd.isDeleted = false
                AND gd.isGoodsDataMarked = true
            ORDER BY gd.modifyDt DESC
                    """)
    List<LatestDataRes> findLatestData(@Param("customerCompanyNo") int customerCompanyNo, Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes(
                p.providerNo,
                p.providerNm,
                g.goodsNo,
                g.goodsNm,
                gd.goodsDataNo,
                gd.goodsDataNm,
                gd.modifyDt
            )
            FROM CustomerCompanyGoodsMapping ccgm
            LEFT JOIN Goods g ON g.goodsNo = ccgm.id.goodsNo
            LEFT JOIN Provider p ON p.providerNo = g.provider.providerNo
            LEFT JOIN GoodsData gd ON gd.goods.goodsNo = g.goodsNo
            LEFT JOIN GoodsDataCategory gdc ON gdc.goodsData.goodsDataNo = gd.goodsDataNo
            JOIN (
                SELECT DISTINCT gdc.goodsData.goodsDataNo as goodsDataNo
                FROM GoodsDataCategory gdc
                WHERE gdc.isDeleted = false
                AND gdc.goodsDataManagementSeparateCd = 'GUDAT'
            ) gud ON gud.goodsDataNo = gd.goodsDataNo
            WHERE ccgm.id.customerCompanyNo = :customerCompanyNo
                AND ccgm.isDeleted = false
                AND ccgm.isUsed = true
                AND g.isDeleted = false
                AND g.isUsed = true
                AND p.isDeleted = false
                AND p.isUsed = true
                AND gd.isDeleted = false
                AND gd.isGoodsDataMarked = true
                AND gdc.isDeleted = false
            GROUP BY p.providerNo, p.providerNm, g.goodsNo, g.goodsNm, gd.goodsDataNo, gd.goodsDataNm
            ORDER BY gd.modifyDt DESC
                    """)
    List<LatestInsightsRes> findLatestInsights(@Param("customerCompanyNo") int customerCompanyNo, Pageable pageable);

}

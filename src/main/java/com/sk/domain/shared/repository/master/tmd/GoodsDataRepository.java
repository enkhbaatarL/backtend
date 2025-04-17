package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.atn.SemanticContext.AND;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataNameRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.mi.dto.response.insights.IGoodsDataRes;
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
    Page<GoodsDataRes> findGoodsDataByProviderNoAndGoodsNoForAdmin(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo,
            @Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataRes(
                cc.customerCompanyNm,
                gd.goodsDataNo,
                gd.goodsDataNm,
                gd.goodsDataExpn
            )
            FROM GoodsData gd
            LEFT JOIN CustomerCompany cc ON gd.customerCompany = cc
            LEFT JOIN CustomerCompanyProviderMapping cp ON gd.goods.provider = cp.provider
            LEFT JOIN CustomerCompanyGoodsMapping cg ON gd.goods = cg.goods
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
                AND gd.customerCompany.customerCompanyNo = :customerCompanyNo
                AND (:keyword IS NULL OR (LOWER(gd.goodsDataNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(gd.goodsDataExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            """)
    Page<GoodsDataRes> findGoodsDataByProviderNoAndGoodsNoForOperator(@Param("customerCompanyNo") int customerCompanyNo,
            @Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo,
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
    List<GoodsDataNameRes> findGoodsDataNamesByProviderNoAndGoodsNoForAdmin(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataNameRes(
            gd.goodsDataNo,
            gd.goodsDataNm
            )
            FROM GoodsData gd
            LEFT JOIN CustomerCompany cc ON gd.customerCompany = cc
            LEFT JOIN CustomerCompanyProviderMapping cp ON gd.goods.provider = cp.provider
            LEFT JOIN CustomerCompanyGoodsMapping cg ON gd.goods = cg.goods
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
            ORDER BY gd.goodsDataNm ASC
                        """, countQuery = """
            SELECT COUNT(DISTINCT gd.goodsDataNo)
            FROM GoodsData gd
            LEFT JOIN CustomerCompany cc ON gd.customerCompany = cc
            LEFT JOIN CustomerCompanyProviderMapping cp ON gd.goods.provider = cp.provider
            LEFT JOIN CustomerCompanyGoodsMapping cg ON gd.goods = cg.goods
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
              """)
    List<GoodsDataNameRes> findGoodsDataNamesByProviderNoAndGoodsNoForOperator(@Param("customerCompanyNo") int customerCompanyNo,
            @Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo);

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

    @EntityGraph(attributePaths = { "goods", "provider" })
    Optional<GoodsData> findByGoodsDataNoAndIsDeletedFalse(@Param("goodsDataNo") int goodsDataNo);

    @Query(value = """
            SELECT
                tg.prvd_no AS providerNo,
                tc.gds_no AS goodsNo,
                tg2.gds_data_no AS goodsDataNo,
                tg2.gds_data_nm AS goodsDataNm,
                tg2.gds_data_expn AS goodsDataExpn,
                tg2.gds_data_smry_cntt AS goodsDataSummaryCntt
            FROM tam_custcpnygdsmappl AS tc
            LEFT JOIN tmd_gdsm AS tg ON tg.gds_no = tc.gds_no
            LEFT JOIN tmd_gdsdatam AS tg2 ON tg2.gds_no = tg.gds_no
            WHERE tc.cust_cpny_no = :customerCompanyNo
              AND tc.del_yn = 'N'
              AND tg.del_yn = 'N'
              AND tg2.del_yn = 'N'
              AND tc.use_yn = 'Y'
              AND tg.use_yn = 'Y'
              AND MATCH(tg2.gds_data_nm, tg2.gds_data_smry_cntt) AGAINST(:keyword IN BOOLEAN MODE)
            ORDER BY tg2.crt_dtm DESC
                        """, countQuery = """
            SELECT
                COUNT(*)
            FROM tam_custcpnygdsmappl AS tc
            LEFT JOIN tmd_gdsm AS tg ON tg.gds_no = tc.gds_no
            LEFT JOIN tmd_gdsdatam AS tg2 ON tg2.gds_no = tg.gds_no
            WHERE tc.cust_cpny_no = :customerCompanyNo
              AND tc.del_yn = 'N'
              AND tg.del_yn = 'N'
              AND tg2.del_yn = 'N'
              AND tc.use_yn = 'Y'
              AND tg.use_yn = 'Y'
              AND MATCH(tg2.gds_data_nm, tg2.gds_data_smry_cntt) AGAINST(:keyword IN BOOLEAN MODE)
            """, nativeQuery = true)
    Page<IGoodsDataRes> findByNameContainingOrSmryCntteContaining(@Param("customerCompanyNo") Integer customerCompanyNo,
            @Param("keyword") String keyword, Pageable pageable);
}

// CONCAT(FUNCTION('GROUP_CONCAT', c.customerCompanyNm)
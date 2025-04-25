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

    @EntityGraph(attributePaths = {"goods", "goods.provider"})
    Optional<GoodsData> findByGoodsDataNoAndIsDeletedFalse(@Param("goodsDataNo") int goodsDataNo);

    //
    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.insights.FocusedDataRes(
                gd.goodsDataNo,
                gdc.goodsDataCategoryCd,
                gd.goodsDataNm,
                gd.modifyDt,
                p.providerNm
            )
            FROM CustomerCompanyProviderMapping cpm
            LEFT JOIN CustomerCompanyGoodsMapping cgm ON cgm.id.customerCompanyNo = cpm.id.customerCompanyNo
            LEFT JOIN Provider p ON p.providerNo = cpm.id.providerNo
            LEFT JOIN GoodsData gd ON gd.goods.goodsNo = cgm.id.goodsNo
            LEFT JOIN GoodsDataCategory gdc ON gdc.goodsData.goodsDataNo = gd.goodsDataNo
            WHERE gdc.goodsDataManagementSeparateCd = 'GDAT'
                AND cpm.isDeleted = false
                AND cpm.isUsed = true
                AND cgm.isDeleted = false
                AND cgm.isUsed = true
                AND p.isDeleted = false
                AND p.isUsed = true
                AND gd.isDeleted = false
                AND gdc.isDeleted = false
                AND cpm.id.customerCompanyNo = :customerCompanyNo
                AND gdc.goodsDataCategoryCd = :categoryCd
            ORDER BY gd.modifyDt DESC
                        """, countQuery = """

            """)
    List<FocusedDataRes> findFocusedDataByCategoryCd(@Param("customerCompanyNo") int customerCompanyNo, @Param("categoryCd") String categoryCd,
            Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.insights.FocusedDataRes(
                gd.goodsDataNo,
                gdc.goodsDataCategoryCd,
                gd.goodsDataNm,
                gd.modifyDt,
                p.providerNm
            )
            FROM CustomerCompanyProviderMapping cpm
            LEFT JOIN CustomerCompanyGoodsMapping cgm ON cgm.id.customerCompanyNo = cpm.id.customerCompanyNo
            LEFT JOIN Provider p ON p.providerNo = cpm.id.providerNo
            LEFT JOIN GoodsData gd ON gd.goods.goodsNo = cgm.id.goodsNo
            LEFT JOIN GoodsDataCategory gdc ON gdc.goodsData.goodsDataNo = gd.goodsDataNo
            WHERE gdc.goodsDataManagementSeparateCd = 'GDAT'
                AND cpm.isDeleted = false
                AND cpm.isUsed = true
                AND cgm.isDeleted = false
                AND cgm.isUsed = true
                AND p.isDeleted = false
                AND p.isUsed = true
                AND gd.isDeleted = false
                AND gdc.isDeleted = false
                AND cpm.id.customerCompanyNo = :customerCompanyNo
                AND gdc.goodsDataCategoryCd <> :categoryCd
            ORDER BY gd.modifyDt DESC
                        """, countQuery = """

            """)
    List<FocusedDataRes> findFocusedDataByNotCategoryCd(@Param("customerCompanyNo") int customerCompanyNo, @Param("categoryCd") String categoryCd,
            Pageable pageable);
}

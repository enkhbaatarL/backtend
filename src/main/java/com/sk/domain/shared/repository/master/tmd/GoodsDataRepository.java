package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataNameRes;
import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

public interface GoodsDataRepository extends JpaRepository<GoodsData, Integer> {
    /**
     * 
     * 기본적으로 고객사나 사용자의 사용가능 상태는 체크 하지 않는다. 로그인 단에서 체크하는걸로 처리
     *
     */

    Optional<GoodsData> findByGoodsDataNoAndIsDeletedFalse(@Param("goodsDataNo") int goodsDataNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataRes(
                gd.goodsDataNo,
                cc.customerCompanyNm,
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
                gd.goodsDataNo,
                cc.customerCompanyNm,
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
                        """)
    List<GoodsDataNameRes> findGoodsDataNamesByProviderNoAndGoodsNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo);

}

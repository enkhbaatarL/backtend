package com.sk.domain.shared.repository.master.tmd;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

public interface GoodsDataRepository extends JpaRepository<GoodsData, Integer> {

        // @Query(value = """
        // SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataRes(
        // gd.goodsDataNo,
        // gd.goodsDataSourceNm,
        // gd.targetDbNm,
        // gd.targetTableNm,
        // gd.queryConditionContent,
        // gd.goodsDataSourceExpn,
        // gd.createUserId,
        // gd.createDt,
        // gd.modifyUserId,
        // gd.modifyDt
        // )
        // FROM GoodsData gd
        // WHERE gd.isDeleted = false
        // AND gd.goods.provider.providerNo = :providerNo
        // AND gd.goods.goodsNo = :goodsNo
        // AND (:keyword IS NULL OR (LOWER(gd.goodsDataSourceNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(gd.goodsDataSourceExpn) LIKE
        // CONCAT('%', LOWER(:keyword), '%')))
        // ORDER BY gd.goodsDataSourceNm ASC, gd.goodsDataSourceExpn ASC
        // """, countQuery = """
        // SELECT COUNT(gd)
        // FROM GoodsDataSource gd
        // WHERE gd.isDeleted = false
        // AND gd.goods.provider.providerNo = :providerNo
        // AND gd.goods.goodsNo = :goodsNo
        // AND (:keyword IS NULL OR (LOWER(gd.goodsDataSourceNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(gd.goodsDataSourceExpn) LIKE
        // CONCAT('%', LOWER(:keyword), '%')))
        // """)
        // Page<GoodsDataRes> searchGoodsDataSources(@Param("providerNo") Integer providerNo, @Param("goodsNo") Integer goodsNo,
        // @Param("keyword") String keyword, Pageable pageable);
}

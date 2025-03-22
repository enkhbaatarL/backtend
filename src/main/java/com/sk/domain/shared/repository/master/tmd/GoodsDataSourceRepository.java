package com.sk.domain.shared.repository.master.tmd;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataSourceRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSource;

public interface GoodsDataSourceRepository extends JpaRepository<GoodsDataSource, Integer> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataSourceRes(
            gds.goodsDataSourceNo,
            gds.goodsDataSourceNm,
            gds.targetDbNm,
            gds.targetTableNm,
            gds.queryConditionContent,
            gds.goodsDataSourceExpn,
            gds.createUserId,
            gds.createDt,
            gds.modifyUserId,
            gds.modifyDt
            )
            FROM GoodsDataSource gds
            WHERE gds.isDeleted = false
            AND gds.goods.provider.providerNo = :providerNo
            AND gds.goods.goodsNo = :goodsNo
            AND (:keyword IS NULL OR (LOWER(gds.goodsDataSourceNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(gds.goodsDataSourceExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            ORDER BY gds.goodsDataSourceNm ASC, gds.goodsDataSourceExpn ASC
                  """, countQuery = """
            SELECT COUNT(gds)
            FROM GoodsDataSource gds
            WHERE gds.isDeleted = false
            AND gds.goods.provider.providerNo = :providerNo
            AND gds.goods.goodsNo = :goodsNo
            AND (:keyword IS NULL OR (LOWER(gds.goodsDataSourceNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(gds.goodsDataSourceExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            """)
    Page<GoodsDataSourceRes> searchGoodsDataSources(@Param("providerNo") Integer providerNo, @Param("goodsNo") Integer goodsNo,
            @Param("keyword") String keyword, Pageable pageable);
}

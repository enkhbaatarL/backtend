package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GoodsRepository extends JpaRepository<Goods, Integer> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsRes(
            g.goodsNo,
            g.goodsNm,
            g.goodsExpn,
            count(gds)
              )
            FROM Goods g
            LEFT JOIN GoodsDataSource gds on g = gds.goods
            WHERE g.isDeleted = false
            AND g.provider.providerNo = :providerNo
            AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            GROUP BY g.goodsNo, g.goodsNm, g.goodsExpn
            ORDER BY g.goodsNm asc, g.goodsExpn asc
                  """, countQuery = """
            SELECT COUNT(distinct g.goodsNo)
            FROM Goods g
            LEFT JOIN GoodsDataSource gds on g = gds.goods
            WHERE g.isDeleted = false
            AND g.provider.providerNo = :providerNo
            AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            """)
    Page<GoodsRes> searchGoods(@Param("providerNo") Integer providerNo, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDetailRes(
            g.goodsNo,
            g.goodsNm,
            g.goodsCd,
            g.goodsExpn,
            g.isUsed,
            g.createUserId,
            g.createDt,
            cu.userNm,
            g.modifyUserId,
            g.modifyDt,
            mu.userNm
            )
            FROM Goods g
            LEFT JOIN User cu on g.createUserId = cu.userLoginId
            LEFT JOIN User mu on g.modifyUserId = mu.userLoginId
            WHERE g.isDeleted = false
            AND g.provider.providerNo = :providerNo
            AND g.goodsNo = :goodsNo
            """)
    Optional<GoodsDetailRes> findByProviderNoAndGoodsNoAndIsDeletedFalseWithUser(@Param("providerNo") Integer providerNo,
            @Param("goodsNo") Integer goodsNo);

    @Query(value = """
            SELECT g
            FROM Goods g
            WHERE g.isDeleted = false
            AND g.provider.providerNo = :providerNo
            AND g.goodsNo = :goodsNo
            """)
    Optional<Goods> findByProviderNoAndGoodsNoAndIsDeletedFalse(Integer providerNo, Integer goodsNo);

}

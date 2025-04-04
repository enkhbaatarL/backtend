package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
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
            g.isPublic,
            g.goodsNm,
            g.goodsExpn,
            count(gd)
              )
            FROM Goods g
            LEFT JOIN GoodsData gd on g = gd.goods
            WHERE g.isDeleted = false
            AND g.provider.providerNo = :providerNo
            AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            GROUP BY g.goodsNo, g.goodsNm, g.goodsExpn
            ORDER BY g.goodsNm asc, g.goodsExpn asc
                  """, countQuery = """
            SELECT COUNT(distinct g.goodsNo)
            FROM Goods g
            LEFT JOIN GoodsData gd on g = gd.goods
            WHERE g.isDeleted = false
            AND g.provider.providerNo = :providerNo
            AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            """)
    Page<GoodsRes> findGoodsByProviderNo(@Param("providerNo") Integer providerNo, @Param("keyword") String keyword, Pageable pageable);

    Optional<Goods> findByGoodsNoAndIsDeletedFalse(Integer goodsNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDetailRes(
            g.goodsNo,
            g.goodsNm,
            g.goodsId,
            g.goodsExpn,
            g.isUsed,
            g.isPublic,
            new com.sk.domain.shared.dto.AuditInfo(
                    g.createUserId,
                    g.createDt,
                    g.modifyUserId,
                    g.modifyDt,
                    new com.sk.domain.shared.dto.AuditExtension(
                        cu.userNm,
                        cu.departmentNm,
                        cuc.customerCompanyNm,
                        mu.userNm,
                        mu.departmentNm,
                        muc.customerCompanyNm
                    )
                )
            )
            FROM Goods g
            LEFT JOIN User cu ON g.createUserId = cu.userLoginId
            LEFT JOIN cu.customerCompany cuc
            LEFT JOIN User mu ON g.modifyUserId = mu.userLoginId
            LEFT JOIN mu.customerCompany muc
            WHERE g.isDeleted = false
            AND g.goodsNo = :goodsNo
            """)
    Optional<GoodsDetailRes> getGoodsDetail(@Param("goodsNo") Integer goodsNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes(
            g.modifyDt,
            u.userNm
            )
            FROM Goods g
            LEFT JOIN User u ON u.userLoginId = g.modifyUserId
            WHERE g.isDeleted = false
            AND g.goodsNo = :goodsNo
            """)
    Optional<GoodsLatestUpdatedRes> getGoodsLatestUpdated(@Param("goodsNo") Integer goodsNo);

}

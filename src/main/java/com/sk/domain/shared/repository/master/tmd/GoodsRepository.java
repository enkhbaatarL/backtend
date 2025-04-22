package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsRepository extends JpaRepository<Goods, Integer> {
    /**
     *
     * 기본적으로 고객사나 사용자의 사용가능 상태는 체크 하지 않는다. 로그인 단에서 체크하는걸로 처리
     *
     */

    Optional<Goods> findByGoodsNoAndIsDeletedFalse(int goodsNo);

    // 관리자는 전체 상품 목록을 조회한다.
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsRes(
                g.goodsNo,
                g.isPublic,
                g.goodsId,
                g.goodsNm,
                g.goodsExpn,
                count(gd)
            )
            FROM Goods g
            LEFT JOIN GoodsData gd on g = gd.goods
            WHERE g.isDeleted = false
              AND g.provider.providerNo = :providerNo
              AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            GROUP BY g.goodsNo, g.isPublic, g.goodsNm, g.goodsExpn
            ORDER BY g.goodsNm ASC, g.goodsExpn ASC
                  """, countQuery = """
            SELECT COUNT(distinct g.goodsNo)
            FROM Goods g
            LEFT JOIN GoodsData gd on g = gd.goods
            WHERE g.isDeleted = false
              AND g.provider.providerNo = :providerNo
              AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
            """)
    Page<GoodsRes> findGoodsByProviderNoForAdmin(@Param("providerNo") int providerNo, @Param("keyword") String keyword, Pageable pageable);

    // 운영자는 고객사와 맵핑된 상품 목록을 조회한다.
    // 고객사와 프로바이더가 맵핑 되어있는 상품 중에
    // 공개된 상품과
    // 고객사에 맵핑된 비공개 상품을 조회한다.
    // 고객사와 프로바이더가 맵핑 되어있지 않은 상품은 조회하지 않는다.
    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsRes(
                g.goodsNo,
                g.isPublic,
                g.goodsId,
                g.goodsNm,
                g.goodsExpn,
                count(gd)
            )
            FROM Goods g
            LEFT JOIN GoodsData gd on g = gd.goods
            WHERE g.isDeleted = false
              AND g.provider.providerNo = :providerNo
              AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                    OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
              AND g.provider IN (
                    SELECT ccp.provider
                    FROM CustomerCompanyProviderMapping ccp
                    WHERE ccp.customerCompany.customerCompanyNo = :customerCompanyNo
              )
              AND (
                    g.isPublic = TRUE
                    OR (g.isPublic = FALSE AND EXISTS (
                        SELECT 1 FROM CustomerCompanyGoodsMapping cgm
                        WHERE cgm.customerCompany.customerCompanyNo = :customerCompanyNo
                          AND cgm.goods = g
                    ))
              )
            GROUP BY g.goodsNo, g.isPublic, g.goodsNm, g.goodsExpn
            ORDER BY g.goodsNm ASC, g.goodsExpn ASC
            """, countQuery = """
            SELECT COUNT(DISTINCT g.goodsNo)
            FROM Goods g
            LEFT JOIN GoodsData gd on g = gd.goods
            WHERE g.isDeleted = false
              AND g.provider.providerNo = :providerNo
              AND (:keyword IS NULL OR (LOWER(g.goodsNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                    OR LOWER(g.goodsExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
              AND g.provider IN (
                    SELECT ccp.provider
                    FROM CustomerCompanyProviderMapping ccp
                    WHERE ccp.customerCompany.customerCompanyNo = :customerCompanyNo
              )
              AND (
                    g.isPublic = TRUE
                    OR (g.isPublic = FALSE AND EXISTS (
                        SELECT 1 FROM CustomerCompanyGoodsMapping cgm
                        WHERE cgm.customerCompany.customerCompanyNo = :customerCompanyNo
                          AND cgm.goods = g
                    ))
              )
            """)
    Page<GoodsRes> findGoodsByProviderNoForOperator(@Param("customerCompanyNo") int customerCompanyNo, @Param("providerNo") int providerNo,
            @Param("keyword") String keyword, Pageable pageable);

    //
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
            LEFT JOIN User cu ON g.createUserId = cu.userLoginId AND (cu.userRoleCd = 'ADMIN' OR cu.userRoleCd = 'OPERATOR')
            LEFT JOIN cu.customerCompany cuc
            LEFT JOIN User mu ON g.modifyUserId = mu.userLoginId AND (mu.userRoleCd = 'ADMIN' OR mu.userRoleCd = 'OPERATOR')
            LEFT JOIN mu.customerCompany muc
            WHERE g.isDeleted = false
            AND g.provider.providerNo = :providerNo
            AND g.goodsNo = :goodsNo
            """)
    Optional<GoodsDetailRes> findGoodsDetail(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes(
            g.modifyDt,
            u.userNm
            )
            FROM Goods g
            LEFT JOIN User u ON u.userLoginId = g.modifyUserId AND (u.userRoleCd = 'ADMIN' OR u.userRoleCd = 'OPERATOR')
            WHERE g.isDeleted = false
            AND g.goodsNo = :goodsNo
            """)
    Optional<GoodsLatestUpdatedRes> findGoodsLatestUpdatedByGoodsNo(@Param("goodsNo") int goodsNo);

    // 관리자는 상품 번호로 상품을 조회한다.
    @Query(value = """
            SELECT g
            FROM Goods g
            WHERE g.isDeleted = false
                AND g.provider.providerNo = :providerNo
                AND g.goodsNo = :goodsNo
            """)
    Optional<Goods> findByGoodsNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo);

    List<Goods> findByProvider_ProviderNoAndIsUsedTrueAndIsDeletedFalse(Integer providerNo);

}

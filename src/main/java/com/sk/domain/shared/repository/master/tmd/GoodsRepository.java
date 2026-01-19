package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.shared.entity.master.tmd.Goods;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsRepository extends JpaRepository<Goods, Integer> {

    /**
     *
     * 기본적으로 고객사나 사용자의 사용가능 상태는 체크 하지 않는다. 로그인 단에서 체크하는걸로 처리
     *
     */

    Optional<Goods> findByGoodsNoAndIsDeletedFalse(int goodsNo);

    @Query(value = """
            SELECT g.goodsNo
            FROM Goods g
            WHERE g.isDeleted = false
              AND g.goodsId = :goodsId
            """)
    Optional<Integer> queryGoodsNoByGoodsId(@Param("goodsId") String goodsId);

    @Query(value = """
            SELECT g.goodsId, g.goodsNo
            FROM Goods g
            WHERE g.isDeleted = false
              AND g.goodsId IN :goodsIds
            """)
    List<Object[]> findGoodsNoByGoodsIdIn(@Param("goodsIds") List<String> goodsIds);

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
    Page<GoodsRes> findGoodsByProviderNo(@Param("providerNo") int providerNo,
        @Param("keyword") String keyword, Pageable pageable);

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
                    COALESCE(cu.userNm, null),
                    COALESCE(cu.departmentNm, null),
                    COALESCE(cuc.customerCompanyNm, null),
                    COALESCE(mu.userNm, null),
                    COALESCE(mu.departmentNm, null),
                    COALESCE(muc.customerCompanyNm, null)
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
    Optional<GoodsDetailRes> findGoodsDetail(@Param("providerNo") int providerNo,
        @Param("goodsNo") int goodsNo);

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
    Optional<Goods> findByGoodsNo(@Param("providerNo") int providerNo,
        @Param("goodsNo") int goodsNo);

    List<Goods> findByProvider_ProviderNoAndIsUsedTrueAndIsDeletedFalse(Integer providerNo);

    @Query(value = """
        SELECT DISTINCT g.gds_no
        FROM tmd_gdsm g
             INNER JOIN tam_custcpnygdsmappl c ON c.gds_no = g.gds_no
                                               AND c.cust_cpny_no = :customerCompanyNo
                                               AND c.use_yn = 'Y'
                                               AND c.del_yn = 'N'
             INNER JOIN tam_authmnagtgdsmappl a ON a.cust_cpny_no = c.cust_cpny_no
                                                AND a.gds_no = c.gds_no
                                                AND a.gds_auth_mnagt_spt_cd = c.gds_auth_mnagt_spt_cd
                                                AND a.gds_auth_cd = 'DOWNLOAD'
                                                AND a.del_yn = 'N'
                                                AND a.use_bgn_dt <= CURDATE()
                                                AND a.use_end_dt >= CURDATE()
                                                AND (c.gds_auth_mnagt_spt_cd = 'COMPANY' OR a.usr_no = :userNo)
             INNER JOIN tam_custcpnyprvdmappl ccpm ON ccpm.cust_cpny_no = c.cust_cpny_no
                                                   AND ccpm.use_yn = 'Y'
                                                   AND ccpm.del_yn = 'N'
        """, nativeQuery = true)
    List<Integer> findGoodsNosWithPermission(
        @Param("customerCompanyNo") Integer customerCompanyNo,
        @Param("userNo") Integer userNo
    );

    /**
     * 권한이 있는 상품번호와 레포지토리 ID를 함께 조회합니다.
     *
     * @param customerCompanyNo 고객회사번호
     * @param userNo            사용자번호
     * @return 상품번호와 레포지토리 ID 리스트
     */
    @Query(value = """
        SELECT DISTINCT g.gds_no, g.vctr_rpstry_id
        FROM tmd_gdsm g
             INNER JOIN tam_custcpnygdsmappl c ON c.gds_no = g.gds_no
                                               AND c.cust_cpny_no = :customerCompanyNo
                                               AND c.use_yn = 'Y'
                                               AND c.del_yn = 'N'
             INNER JOIN tam_authmnagtgdsmappl a ON a.cust_cpny_no = c.cust_cpny_no
                                                AND a.gds_no = c.gds_no
                                                AND a.gds_auth_mnagt_spt_cd = c.gds_auth_mnagt_spt_cd
                                                AND a.gds_auth_cd = 'DOWNLOAD'
                                                AND a.del_yn = 'N'
                                                AND a.use_bgn_dt <= CURDATE()
                                                AND a.use_end_dt >= CURDATE()
                                                AND (c.gds_auth_mnagt_spt_cd = 'COMPANY' OR a.usr_no = :userNo)
             INNER JOIN tam_custcpnyprvdmappl ccpm ON ccpm.cust_cpny_no = c.cust_cpny_no
                                                   AND ccpm.use_yn = 'Y'
                                                   AND ccpm.del_yn = 'N'
        """, nativeQuery = true)
    List<Object[]> findGoodsNosAndRepositoryIdsWithPermission(
        @Param("customerCompanyNo") Integer customerCompanyNo,
        @Param("userNo") Integer userNo
    );
}

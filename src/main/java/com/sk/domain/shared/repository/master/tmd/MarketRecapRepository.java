package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRecapRepository extends JpaRepository<MarketRecap, Long> {

    Optional<MarketRecap> findByCrtrDtAndNwsMrktCtgrCd(String crtrDt, String nwsMrktCtgrCd);

    /**
     * 마켓 리캡 목록 조회 (날짜 범위, 키워드 검색, 페이징)
     *
     * @param startDate 시작일 (yyyyMMdd)
     * @param endDate 종료일 (yyyyMMdd)
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 마켓 리캡 페이지
     */
    @Query(value = """
        SELECT mr
        FROM MarketRecap mr
        WHERE (:startDate IS NULL OR mr.crtrDt >= :startDate)
          AND (:endDate IS NULL OR mr.crtrDt <= :endDate)
          AND (:marketCategoryCode IS NULL OR mr.nwsMrktCtgrCd = :marketCategoryCode)
          AND (:keyword IS NULL OR :keyword = ''
               OR mr.ckptTitl LIKE %:keyword%
               OR mr.hlEvntTitl LIKE %:keyword%)
          AND mr.isDeleted = false
        """)
    Page<MarketRecap> findMarketRecaps(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("marketCategoryCode") String marketCategoryCode,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT
                recap.mrkt_rcap_id AS mrktRcapId,
                recap.crtr_dt AS crtrDt,
                recap.nws_mrkt_ctgr_cd AS nwsMrktCtgrCd,
                recap.recap_category AS recapCategory
            FROM (
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'HIGHLIGHT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_hl_evnt mhe
                          WHERE mhe.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mhe.del_yn = 'N'
                            AND LOWER(mhe.nm_en) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
                UNION ALL
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'CHECKPOINT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_ckpt mc
                          WHERE mc.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mc.del_yn = 'N'
                            AND LOWER(mc.titl) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
            ) recap
            ORDER BY
                recap.crtr_dt DESC,
                CASE WHEN recap.recap_category = 'HIGHLIGHT' THEN 0 ELSE 1 END ASC,
                recap.mrkt_rcap_id DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_hl_evnt mhe
                          WHERE mhe.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mhe.del_yn = 'N'
                            AND LOWER(mhe.nm_en) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
                UNION ALL
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_ckpt mc
                          WHERE mc.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mc.del_yn = 'N'
                            AND LOWER(mc.titl) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
            ) recap
            """,
        nativeQuery = true
    )
    Page<MarketRecapListRow> findMarketRecapRowsNewest(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("includeHighlight") boolean includeHighlight,
        @Param("includeCheckpoint") boolean includeCheckpoint,
        @Param("marketCategoryCode") String marketCategoryCode,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT
                recap.mrkt_rcap_id AS mrktRcapId,
                recap.crtr_dt AS crtrDt,
                recap.nws_mrkt_ctgr_cd AS nwsMrktCtgrCd,
                recap.recap_category AS recapCategory
            FROM (
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'HIGHLIGHT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_hl_evnt mhe
                          WHERE mhe.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mhe.del_yn = 'N'
                            AND LOWER(mhe.nm_en) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
                UNION ALL
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'CHECKPOINT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_ckpt mc
                          WHERE mc.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mc.del_yn = 'N'
                            AND LOWER(mc.titl) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
            ) recap
            ORDER BY
                recap.crtr_dt ASC,
                CASE WHEN recap.recap_category = 'HIGHLIGHT' THEN 0 ELSE 1 END ASC,
                recap.mrkt_rcap_id DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_hl_evnt mhe
                              JOIN tmd_mrkt_rcap_hl_evnt_rel mher
                                  ON mher.mrkt_rcap_hl_evnt_id = mhe.mrkt_rcap_hl_evnt_id
                                  AND mher.del_yn = 'N'
                              JOIN tmd_nws_evnt ne
                                  ON ne.nws_evnt_id = mher.nws_evnt_id
                                  AND ne.del_yn = 'N'
                              JOIN tmd_nws_smry ns
                                  ON ns.nws_smry_id = ne.nws_smry_id
                                  AND ns.del_yn = 'N'
                              JOIN tif_refinewsdatar rnd
                                  ON rnd.data_reg_sn = ns.data_reg_sn
                                  AND rnd.first_created = ns.first_created
                                  AND rnd.del_yn = 'N'
                          WHERE mhe.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mhe.del_yn = 'N'
                            AND LOWER(rnd.title) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
                UNION ALL
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR EXISTS (
                          SELECT 1
                          FROM tmd_mrkt_rcap_ckpt mc
                          WHERE mc.mrkt_rcap_id = mr.mrkt_rcap_id
                            AND mc.del_yn = 'N'
                            AND LOWER(mc.titl) LIKE CONCAT('%', LOWER(:keyword), '%')
                      )
                  )
            ) recap
            """,
        nativeQuery = true
    )
    Page<MarketRecapListRow> findMarketRecapRowsOldest(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("includeHighlight") boolean includeHighlight,
        @Param("includeCheckpoint") boolean includeCheckpoint,
        @Param("marketCategoryCode") String marketCategoryCode,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT
                recap.mrkt_rcap_id AS mrktRcapId,
                recap.crtr_dt AS crtrDt,
                recap.nws_mrkt_ctgr_cd AS nwsMrktCtgrCd,
                recap.recap_category AS recapCategory
            FROM (
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'HIGHLIGHT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                UNION ALL
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'CHECKPOINT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
            ) recap
            ORDER BY
                recap.crtr_dt DESC,
                CASE WHEN recap.recap_category = 'HIGHLIGHT' THEN 0 ELSE 1 END ASC,
                recap.mrkt_rcap_id DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                UNION ALL
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:marketCategoryCode IS NULL OR mr.nws_mrkt_ctgr_cd = :marketCategoryCode)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
            ) recap
            """,
        nativeQuery = true
    )
    Page<MarketRecapListRow> findMarketRecapRowsNewestWithoutKeyword(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("includeHighlight") boolean includeHighlight,
        @Param("includeCheckpoint") boolean includeCheckpoint,
        @Param("marketCategoryCode") String marketCategoryCode,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT
                recap.mrkt_rcap_id AS mrktRcapId,
                recap.crtr_dt AS crtrDt,
                recap.nws_mrkt_ctgr_cd AS nwsMrktCtgrCd,
                recap.recap_category AS recapCategory
            FROM (
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'HIGHLIGHT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                UNION ALL
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    'CHECKPOINT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
            ) recap
            ORDER BY
                recap.crtr_dt ASC,
                CASE WHEN recap.recap_category = 'HIGHLIGHT' THEN 0 ELSE 1 END ASC,
                recap.mrkt_rcap_id DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                UNION ALL
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
            ) recap
            """,
        nativeQuery = true
    )
    Page<MarketRecapListRow> findMarketRecapRowsOldestWithoutKeyword(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("includeHighlight") boolean includeHighlight,
        @Param("includeCheckpoint") boolean includeCheckpoint,
        @Param("marketCategoryCode") String marketCategoryCode,
        Pageable pageable
    );

}

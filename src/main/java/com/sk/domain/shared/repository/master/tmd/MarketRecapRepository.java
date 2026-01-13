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
          AND (:keyword IS NULL OR :keyword = ''
               OR mr.ckptTitl LIKE %:keyword%
               OR mr.hlEvntTitl LIKE %:keyword%)
          AND mr.isDeleted = false
        """)
    Page<MarketRecap> findMarketRecaps(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT
                recap.mrkt_rcap_id AS mrktRcapId,
                recap.crtr_dt AS crtrDt,
                recap.nws_mrkt_ctgr_cd AS nwsMrktCtgrCd,
                recap.hl_evnt_titl AS hlEvntTitl,
                recap.hl_evnt_cntt AS hlEvntCntt,
                recap.ckpt_titl AS ckptTitl,
                recap.ckpt_cntt AS ckptCntt,
                recap.recap_category AS recapCategory
            FROM (
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    mr.hl_evnt_titl,
                    mr.hl_evnt_cntt,
                    mr.ckpt_titl,
                    mr.ckpt_cntt,
                    'HIGHLIGHT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:keyword IS NULL OR :keyword = ''
                       OR mr.ckpt_titl LIKE %:keyword%
                       OR mr.hl_evnt_titl LIKE %:keyword%)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                UNION ALL
                SELECT
                    mr.mrkt_rcap_id,
                    mr.crtr_dt,
                    mr.nws_mrkt_ctgr_cd,
                    mr.hl_evnt_titl,
                    mr.hl_evnt_cntt,
                    mr.ckpt_titl,
                    mr.ckpt_cntt,
                    'CHECKPOINT' AS recap_category
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:keyword IS NULL OR :keyword = ''
                       OR mr.ckpt_titl LIKE %:keyword%
                       OR mr.hl_evnt_titl LIKE %:keyword%)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
            ) recap
            ORDER BY
                CASE WHEN :isNewest = true THEN recap.crtr_dt END DESC,
                CASE WHEN :isNewest = false THEN recap.crtr_dt END ASC,
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
                  AND (:keyword IS NULL OR :keyword = ''
                       OR mr.ckpt_titl LIKE %:keyword%
                       OR mr.hl_evnt_titl LIKE %:keyword%)
                  AND mr.del_yn = 'N'
                  AND :includeHighlight = true
                UNION ALL
                SELECT mr.mrkt_rcap_id
                FROM tmd_mrkt_rcap mr
                WHERE (:startDate IS NULL OR mr.crtr_dt >= :startDate)
                  AND (:endDate IS NULL OR mr.crtr_dt <= :endDate)
                  AND (:keyword IS NULL OR :keyword = ''
                       OR mr.ckpt_titl LIKE %:keyword%
                       OR mr.hl_evnt_titl LIKE %:keyword%)
                  AND mr.del_yn = 'N'
                  AND :includeCheckpoint = true
            ) recap
            """,
        nativeQuery = true
    )
    Page<MarketRecapListRow> findMarketRecapRows(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("includeHighlight") boolean includeHighlight,
        @Param("includeCheckpoint") boolean includeCheckpoint,
        @Param("isNewest") boolean isNewest,
        Pageable pageable
    );

}

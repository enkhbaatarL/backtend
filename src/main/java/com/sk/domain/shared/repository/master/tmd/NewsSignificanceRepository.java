package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.mi.dto.response.news.NewsfeedListRowProjection;
import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsSignificanceRepository extends JpaRepository<NewsSignificance, Long>,
    JpaSpecificationExecutor<NewsSignificance> {

    /**
     * Newsfeed 목록 조회 (HEAD, NONE만 조회)
     * - HEAD: tmd_simlr_nws_grp에 존재하는 뉴스
     * - NONE: tmd_simlr_nws_grp, tmd_simlr_nws_grp_rel 모두에 없는 뉴스
     */
    @Query(value = """
        SELECT
            ns.nws_sig_id AS newsSignificanceId,
            nsum.data_reg_sn AS dataRegistrationSerialNo,
            nsum.first_created AS firstCreated,
            ns.nws_mrkt_ctgr_cd AS newsMarketCategoryCode,
            ns.scre AS newsSignificanceScore,
            sn.sgt_nws_id AS newsSignalId,
            sn.scre AS significantNewsScore,
            nsum.nws_smry_id AS newsSummaryId,
            nsum.simlr_grp_stat_cd AS similarityGroupStatusCode,
            nsum.brf_smry AS briefSummary,
            nsum.smry AS summary,
            nsum.prvd_id AS provider,
            nsum.gds_id AS goodsId,
            rnd.title AS title,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') AS regions
        FROM tmd_sgt_nws sn
        LEFT JOIN tmd_nws_sig ns ON sn.nws_sig_id = ns.nws_sig_id
        LEFT JOIN tmd_nws_smry nsum ON ns.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = nsum.nws_smry_id
        WHERE (sn.crtr_dt = :criteriaDate)
          AND (ns.nws_mrkt_ctgr_cd = :category)
          AND (nsum.simlr_grp_stat_cd IN ('HEAD', 'NONE'))
          AND nsum.del_yn = 'N'
          AND (:minScore IS NULL OR ns.scre >= :minScore)
          AND (:filterBySentiment = FALSE OR (
                                     /* UPWARD: 3.0 <= score <= 10.0 */
                                     ('upward' IN (:sentimentList) AND sn.scre >= 3.0 AND sn.scre <= 10.0)
                                  OR /* NEUTRAL: -3.0 < score < 3.0 */
                                     ('neutral' IN (:sentimentList) AND sn.scre > -3.0 AND sn.scre < 3.0)
                                  OR /* DOWNWARD: -10.0 <= score <= -3.0 */
                                     ('downward' IN (:sentimentList) AND sn.scre >= -10.0 AND sn.scre <= -3.0)
                              ))
          AND (:filterByImpactScore = FALSE OR (
              ('HIGH' IN (:impactScoreList) AND ns.scre >= 9.0)
              OR ('MEDIUM' IN (:impactScoreList) AND ns.scre >= 7.0 AND ns.scre < 9.0)
              OR ('LOW' IN (:impactScoreList) AND ns.scre >= 5.0 AND ns.scre < 7.0)
          ))
          AND (:filterByRegion = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_rgn_cls nrc2
              WHERE nrc2.nws_smry_id = nsum.nws_smry_id
              AND nrc2.nws_rgn_spt_cd IN (:regionList)
          ))
        GROUP BY
            ns.nws_sig_id,
            nsum.data_reg_sn,
            nsum.first_created,
            ns.nws_mrkt_ctgr_cd,
            ns.scre,
            sn.sgt_nws_id,
            sn.scre,
            nsum.nws_smry_id,
            nsum.simlr_grp_stat_cd,
            nsum.brf_smry,
            nsum.smry,
            nsum.prvd_id,
            nsum.gds_id,
            rnd.title
        ORDER BY
            CASE WHEN :sortField = 'firstCreated' AND :sortDirection = 'ASC' THEN nsum.first_created END ASC,
            CASE WHEN :sortField = 'firstCreated' AND :sortDirection = 'DESC' THEN nsum.first_created END DESC,
            CASE WHEN :sortField = 'impctScre' AND :sortDirection = 'ASC' THEN ns.scre END ASC,
            CASE WHEN :sortField = 'impctScre' AND :sortDirection = 'DESC' THEN ns.scre END DESC,
            nsum.data_reg_sn DESC
        """, nativeQuery = true)
    List<NewsfeedListRowProjection> findNewsfeedWithGroupConcat(
        @Param("criteriaDate") String criteriaDate,
        @Param("category") String category,
        @Param("minScore") BigDecimal minScore,
        @Param("sentimentList") List<String> sentimentList,
        @Param("filterBySentiment") boolean filterBySentiment,
        @Param("impactScoreList") List<String> impactScoreList,
        @Param("filterByImpactScore") boolean filterByImpactScore,
        @Param("regionList") List<String> regionList,
        @Param("filterByRegion") boolean filterByRegion,
        @Param("sortField") String sortField,
        @Param("sortDirection") String sortDirection,
        Pageable pageable
    );

    /**
     * 특정 HEAD 뉴스의 유사 뉴스(INCL) 목록 조회
     *
     * @param headNewsSummaryIds HEAD 뉴스들의 nws_smry_id 목록
     * @return [simlr_nws_grp_id, head_nws_smry_id, incl_nws_smry_id, title, first_created, data_reg_sn, provider_id, goods_id]
     */
    @Query(value = """
        SELECT
            sng.simlr_nws_grp_id,
            sng.nws_smry_id as head_nws_smry_id,
            nsum.nws_smry_id as incl_nws_smry_id,
            rnd.title,
            nsum.first_created,
            nsum.data_reg_sn,
            nsum.prvd_id,
            nsum.gds_id
        FROM tmd_simlr_nws_grp sng
        INNER JOIN tmd_simlr_nws_grp_rel sngr ON sng.simlr_nws_grp_id = sngr.simlr_nws_grp_id
        INNER JOIN tmd_nws_smry nsum ON sngr.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        WHERE sng.nws_smry_id IN (:headNewsSummaryIds)
          AND sng.del_yn = 'N'
          AND sngr.del_yn = 'N'
          AND nsum.del_yn = 'N'
        ORDER BY sng.nws_smry_id, nsum.first_created DESC
        """, nativeQuery = true)
    List<Object[]> findSimilarNewsByHeadIds(@Param("headNewsSummaryIds") List<Long> headNewsSummaryIds);

    /**
     * Market News 목록 조회 (GROUP_CONCAT 사용)
     */
    @Query(value = """
        SELECT
            nsum.nws_smry_id,
            nsum.data_reg_sn,
            nsum.first_created,
            rnd.title,
            nsum.prvd_id,
            nsum.gds_id,
            GROUP_CONCAT(DISTINCT ns.nws_mrkt_ctgr_cd SEPARATOR ',') as categories,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') as regions,
            GROUP_CONCAT(DISTINCT nic.nws_ind_sctr_cd SEPARATOR ',') as industries
        FROM tmd_nws_smry nsum
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        LEFT JOIN tmd_nws_sig ns ON ns.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tmd_nws_ind_cls nic ON nic.nws_smry_id = nsum.nws_smry_id
        WHERE 1=1
          AND (:keyword IS NULL OR :keyword = ''
              OR UPPER(rnd.title) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(rnd.story) LIKE UPPER(CONCAT('%', :keyword, '%')))
          AND (:startDateTime IS NULL OR nsum.first_created >= :startDateTime)
          AND (:endDateTime IS NULL OR nsum.first_created <= :endDateTime)
          AND (:filterByCategory = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_sig ns2
              WHERE ns2.nws_smry_id = nsum.nws_smry_id
              AND ns2.nws_mrkt_ctgr_cd IN (:categories)
          ))
          AND (:filterBySentiment = FALSE OR EXISTS (
              SELECT 1
              FROM tmd_nws_sig ns2
              JOIN tmd_sgt_nws sn2 ON sn2.nws_sig_id = ns2.nws_sig_id
              WHERE ns2.nws_smry_id = nsum.nws_smry_id
                AND (
                    ('upward' IN (:sentiments) AND sn2.scre >= 3.0 AND sn2.scre <= 10.0)
                    OR ('neutral' IN (:sentiments) AND sn2.scre > -3.0 AND sn2.scre < 3.0)
                    OR ('downward' IN (:sentiments) AND sn2.scre >= -10.0 AND sn2.scre <= -3.0)
                )
          ))
          AND (:filterByRegion = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_rgn_cls nrc2
              WHERE nrc2.nws_smry_id = nsum.nws_smry_id
              AND nrc2.nws_rgn_spt_cd IN (:regions)
          ))
          AND (:filterByIndustry = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_ind_cls nic2
              WHERE nic2.nws_smry_id = nsum.nws_smry_id
              AND nic2.nws_ind_sctr_cd IN (:industries)
          ))
        GROUP BY
            nsum.nws_smry_id,
            nsum.data_reg_sn,
            nsum.first_created,
            rnd.title,
            nsum.prvd_id,
            nsum.gds_id
        ORDER BY
            CASE WHEN :sortDirection = 'ASC' THEN nsum.first_created END ASC,
            CASE WHEN :sortDirection = 'DESC' THEN nsum.first_created END DESC,
            nsum.data_reg_sn DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findMarketNewsListWithGroupConcat(
        @Param("keyword") String keyword,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("sentiments") List<String> sentiments,
        @Param("filterBySentiment") boolean filterBySentiment,
        @Param("regions") List<String> regions,
        @Param("filterByRegion") boolean filterByRegion,
        @Param("industries") List<String> industries,
        @Param("filterByIndustry") boolean filterByIndustry,
        @Param("sortDirection") String sortDirection,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * Market News 총 개수 조회
     */
    @Query(value = """
        SELECT COUNT(DISTINCT nsum.nws_smry_id)
        FROM tmd_nws_smry nsum
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        WHERE 1=1
          AND (:keyword IS NULL OR :keyword = ''
              OR UPPER(rnd.title) LIKE UPPER(CONCAT('%', :keyword, '%'))
              OR UPPER(rnd.story) LIKE UPPER(CONCAT('%', :keyword, '%')))
          AND (:startDateTime IS NULL OR nsum.first_created >= :startDateTime)
          AND (:endDateTime IS NULL OR nsum.first_created <= :endDateTime)
          AND (:filterByCategory = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_sig ns2
              WHERE ns2.nws_smry_id = nsum.nws_smry_id
              AND ns2.nws_mrkt_ctgr_cd IN (:categories)
          ))
          AND (:filterBySentiment = FALSE OR EXISTS (
              SELECT 1
              FROM tmd_nws_sig ns2
              JOIN tmd_sgt_nws sn2 ON sn2.nws_sig_id = ns2.nws_sig_id
              WHERE ns2.nws_smry_id = nsum.nws_smry_id
                AND (
                    ('upward' IN (:sentiments) AND sn2.scre >= 3.0 AND sn2.scre <= 10.0)
                    OR ('neutral' IN (:sentiments) AND sn2.scre > -3.0 AND sn2.scre < 3.0)
                    OR ('downward' IN (:sentiments) AND sn2.scre >= -10.0 AND sn2.scre <= -3.0)
                )
          ))
          AND (:filterByRegion = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_rgn_cls nrc
              WHERE nrc.nws_smry_id = nsum.nws_smry_id
              AND nrc.nws_rgn_spt_cd IN (:regions)
          ))
          AND (:filterByIndustry = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_ind_cls nic
              WHERE nic.nws_smry_id = nsum.nws_smry_id
              AND nic.nws_ind_sctr_cd IN (:industries)
          ))
        """, nativeQuery = true)
    long countMarketNewsList(
        @Param("keyword") String keyword,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("sentiments") List<String> sentiments,
        @Param("filterBySentiment") boolean filterBySentiment,
        @Param("regions") List<String> regions,
        @Param("filterByRegion") boolean filterByRegion,
        @Param("industries") List<String> industries,
        @Param("filterByIndustry") boolean filterByIndustry
    );

    /**
     * 뉴스 요약 ID로 중요도 조회
     */
    List<NewsSignificance> findByNewsSummaryId(Long newsSummaryId);
}

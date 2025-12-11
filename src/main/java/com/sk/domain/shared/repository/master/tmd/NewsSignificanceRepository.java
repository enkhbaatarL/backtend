package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsSignificanceRepository extends JpaRepository<NewsSignificance, Long>,
    JpaSpecificationExecutor<NewsSignificance> {

    /**
     * Newsfeed 목록 조회
     */
    @Query(value = """
        SELECT
            ns.nws_sig_id,
            nsum.data_reg_sn,
            nsum.first_created,
            ns.nws_mrkt_ctgr_cd,
            ns.scre,
            sn.nws_sig_id as sn_nws_sig_id,
            sn.scre as sn_scre,
            nsum.nws_smry_id,
            nsum.brf_smry,
            nsum.smry,
            nsum.prvd_id,
            rnd.title,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') as regions
        FROM tmd_nws_sig ns
        INNER JOIN tmd_nws_smry nsum ON nsum.nws_smry_id = ns.nws_smry_id
        LEFT JOIN tmd_sgt_nws sn ON sn.nws_sig_id = ns.nws_sig_id
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = nsum.nws_smry_id
        WHERE (:criteriaDate IS NULL OR ns.crtr_dt = :criteriaDate)
          AND (:category IS NULL OR ns.nws_mrkt_ctgr_cd = :category)
          AND (:filterBySentiment = FALSE OR (:sentimentList IS NOT NULL))
          AND ns.scre >= 6.0
          AND (:filterByImpactScore = FALSE OR (
              ('HIGH' IN (:impactScoreList) AND ns.scre >= 9.0)
              OR ('MEDIUM' IN (:impactScoreList) AND ns.scre >= 7.0 AND ns.scre < 9.0)
              OR ('LOW' IN (:impactScoreList) AND ns.scre < 7.0)
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
            sn.nws_sig_id,
            sn.scre,
            nsum.nws_smry_id,
            nsum.brf_smry,
            nsum.smry,
            nsum.prvd_id,
            rnd.title
        ORDER BY
            CASE WHEN :sortField = 'firstCreated' AND :sortDirection = 'ASC' THEN nsum.first_created END ASC,
            CASE WHEN :sortField = 'firstCreated' AND :sortDirection = 'DESC' THEN nsum.first_created END DESC,
            CASE WHEN :sortField = 'impctScre' AND :sortDirection = 'ASC' THEN ns.scre END ASC,
            CASE WHEN :sortField = 'impctScre' AND :sortDirection = 'DESC' THEN ns.scre END DESC,
            nsum.data_reg_sn DESC
        """, nativeQuery = true)
    List<Object[]> findNewsfeedWithGroupConcat(
        @Param("criteriaDate") String criteriaDate,
        @Param("category") String category,
        @Param("sentimentList") List<String> sentimentList,
        @Param("filterBySentiment") boolean filterBySentiment,
        @Param("impactScoreList") List<String> impactScoreList,
        @Param("filterByImpactScore") boolean filterByImpactScore,
        @Param("regionList") List<String> regionList,
        @Param("filterByRegion") boolean filterByRegion,
        @Param("sortField") String sortField,
        @Param("sortDirection") String sortDirection
    );

    /**
     * Market News 목록 조회 (GROUP_CONCAT 사용)
     * 변경: data_reg_sn, first_created JOIN → nws_smry_id 직접 JOIN
     */
    @Query(value = """
        SELECT
            nsum.nws_smry_id,
            nsum.data_reg_sn,
            nsum.first_created,
            rnd.title,
            nsum.prvd_id,
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
            nsum.prvd_id
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
     * 변경: data_reg_sn, first_created JOIN → nws_smry_id 직접 JOIN
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

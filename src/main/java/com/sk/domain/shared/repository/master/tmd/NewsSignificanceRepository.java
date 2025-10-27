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

    @Query(value = """
        SELECT
            ns.nws_sig_id,
            ns.data_reg_sn,
            ns.first_created,
            ns.nws_mrkt_ctgr_cd,
            sn.nws_sig_id as sn_nws_sig_id,
            sn.impct_scre,
            sn.drctnlt,
            nsum.nws_smry_id,
            nsum.brf_smry,
            nsum.smry,
            nsum.provider,
            rnd.title,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') as regions,
            GROUP_CONCAT(DISTINCT nic.nws_ind_sctr_cd SEPARATOR ',') as industries
        FROM tmd_nws_sig ns
        INNER JOIN tmd_sgt_nws sn ON sn.nws_sig_id = ns.nws_sig_id
        LEFT JOIN tmd_nws_smry nsum ON nsum.data_reg_sn = ns.data_reg_sn
            AND nsum.first_created = ns.first_created
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tmd_nws_ind_cls nic ON nic.nws_smry_id = nsum.nws_smry_id
        WHERE (:startDateTime IS NULL OR ns.first_created >= :startDateTime)
          AND (:endDateTime IS NULL OR ns.first_created <= :endDateTime)
          AND (:category IS NULL OR ns.nws_mrkt_ctgr_cd = :category)
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
            ns.nws_sig_id,
            ns.data_reg_sn,
            ns.first_created,
            ns.nws_mrkt_ctgr_cd,
            sn.nws_sig_id,
            sn.impct_scre,
            sn.drctnlt,
            nsum.nws_smry_id,
            nsum.brf_smry,
            nsum.smry,
            nsum.provider,
            rnd.title
        ORDER BY
            CASE WHEN :sortField = 'firstCreated' AND :sortDirection = 'ASC' THEN ns.first_created END ASC,
            CASE WHEN :sortField = 'firstCreated' AND :sortDirection = 'DESC' THEN ns.first_created END DESC,
            CASE WHEN :sortField = 'impctScre' AND :sortDirection = 'ASC' THEN sn.impct_scre END ASC,
            CASE WHEN :sortField = 'impctScre' AND :sortDirection = 'DESC' THEN sn.impct_scre END DESC,
            ns.data_reg_sn DESC
        """, nativeQuery = true)
    List<Object[]> findNewsfeedWithGroupConcat(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("category") String category,
        @Param("regions") List<String> regions,
        @Param("filterByRegion") boolean filterByRegion,
        @Param("industries") List<String> industries,
        @Param("filterByIndustry") boolean filterByIndustry,
        @Param("sortField") String sortField,
        @Param("sortDirection") String sortDirection
    );


    @Query(value = """
        SELECT
            nsum.nws_smry_id,
            nsum.data_reg_sn,
            nsum.first_created,
            rnd.title,
            nsum.provider,
            GROUP_CONCAT(DISTINCT ns.nws_mrkt_ctgr_cd SEPARATOR ',') as categories,
            GROUP_CONCAT(DISTINCT nrc.nws_rgn_spt_cd SEPARATOR ',') as regions,
            GROUP_CONCAT(DISTINCT nic.nws_ind_sctr_cd SEPARATOR ',') as industries
        FROM tmd_nws_smry nsum
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        LEFT JOIN tmd_nws_sig ns ON ns.data_reg_sn = nsum.data_reg_sn
            AND ns.first_created = nsum.first_created
        LEFT JOIN tmd_nws_rgn_cls nrc ON nrc.nws_smry_id = nsum.nws_smry_id
        LEFT JOIN tmd_nws_ind_cls nic ON nic.nws_smry_id = nsum.nws_smry_id
        WHERE 1=1
          AND (:keyword IS NULL OR rnd.title LIKE CONCAT('%', :keyword, '%') OR rnd.story LIKE CONCAT('%', :keyword, '%'))
          AND (:startDateTime IS NULL OR nsum.first_created >= :startDateTime)
          AND (:endDateTime IS NULL OR nsum.first_created <= :endDateTime)
          AND (:filterByCategory = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_sig ns2
              WHERE ns2.data_reg_sn = nsum.data_reg_sn
              AND ns2.first_created = nsum.first_created
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
            nsum.provider
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

    @Query(value = """
        SELECT COUNT(DISTINCT nsum.nws_smry_id)
        FROM tmd_nws_smry nsum
        LEFT JOIN tif_refinewsdatar rnd ON rnd.data_reg_sn = nsum.data_reg_sn
            AND rnd.first_created = nsum.first_created
        WHERE 1=1
          AND (:keyword IS NULL OR rnd.title LIKE CONCAT('%', :keyword, '%') OR rnd.story LIKE CONCAT('%', :keyword, '%'))
          AND (:startDateTime IS NULL OR nsum.first_created >= :startDateTime)
          AND (:endDateTime IS NULL OR nsum.first_created <= :endDateTime)
          AND (:filterByCategory = FALSE OR EXISTS (
              SELECT 1 FROM tmd_nws_sig ns2
              WHERE ns2.data_reg_sn = nsum.data_reg_sn
              AND ns2.first_created = nsum.first_created
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
}

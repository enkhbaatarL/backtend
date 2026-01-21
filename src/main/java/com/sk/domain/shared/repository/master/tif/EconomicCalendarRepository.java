package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarDetailRes;
import com.sk.domain.shared.entity.master.tif.EconomicCalendar;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EconomicCalendarRepository extends JpaRepository<EconomicCalendar, Long> {

    @Query(
        value = """
            SELECT
                ec.ecnmc_cal_id,
                ec.code,
                ec.date,
                ec.ecnmc_indcr_nm,
                ec.ecnmc_indcr_prsnttn_day,
                ec.ecnmc_indcr_prsnttn_tm,
                ec.ecnmc_indcr_rl_val,
                ec.ecnmc_indcr_prdc_day,
                ec.ecnmc_indcr_prsnttn_pr_val,
                ec.ecnmc_indcr_prdc_val,
                ec.unit_prefix,
                ec.unit,
                rec.prsnttn_rgn,
                rec.priory,
                rec.type,
                'ANNOUNCED' as status
            FROM tif_ecnmc_cal ec
            LEFT JOIN tif_refiecnmicclndrr rec ON ec.code = rec.code
            WHERE ec.ecnmc_indcr_prsnttn_day >= :startDate
              AND ec.ecnmc_indcr_prsnttn_day <= :endDate
              AND ec.del_yn = 'N'
              AND ec.ecnmc_indcr_prsnttn_tm IS NOT NULL
              AND ec.ecnmc_indcr_prsnttn_tm <> ''
              AND (:keyword IS NULL OR :keyword = ''
                   OR UPPER(ec.ecnmc_indcr_nm) LIKE UPPER(CONCAT('%', :keyword, '%')))
              AND (:filterByCategory = FALSE OR rec.type IN :categories)
              AND (:filterByImportance = FALSE OR rec.priory IN :priorities)
            UNION ALL
            SELECT
                ec.ecnmc_cal_id,
                ec.code,
                ec.date,
                ec.ecnmc_indcr_nm,
                ec.nxt_ecnmc_indcr_prsnttn_day as ecnmc_indcr_prsnttn_day,
                ec.ecnmc_indcr_prsnttn_tm,
                ec.ecnmc_indcr_rl_val,
                ec.ecnmc_indcr_prdc_day,
                ec.ecnmc_indcr_prsnttn_pr_val,
                ec.ecnmc_indcr_prdc_val,
                ec.unit_prefix,
                ec.unit,
                rec.prsnttn_rgn,
                rec.priory,
                rec.type,
                'SCHEDULED' as status
            FROM tif_ecnmc_cal ec
            LEFT JOIN tif_refiecnmicclndrr rec ON ec.code = rec.code
            WHERE ec.nxt_ecnmc_indcr_prsnttn_day >= :startDate
              AND ec.nxt_ecnmc_indcr_prsnttn_day <= :endDate
              AND ec.del_yn = 'N'
              AND ec.ecnmc_indcr_prsnttn_tm IS NOT NULL
              AND ec.ecnmc_indcr_prsnttn_tm <> ''
              AND NOT EXISTS (
                  SELECT 1
                  FROM tif_ecnmc_cal announced
                  WHERE announced.code = ec.code
                    AND announced.ecnmc_indcr_prsnttn_day = ec.nxt_ecnmc_indcr_prsnttn_day
                    AND announced.ecnmc_indcr_prsnttn_day >= :startDate
                    AND announced.ecnmc_indcr_prsnttn_day <= :endDate
                    AND announced.del_yn = 'N'
              )
              AND (:keyword IS NULL OR :keyword = ''
                   OR UPPER(ec.ecnmc_indcr_nm) LIKE UPPER(CONCAT('%', :keyword, '%')))
              AND (:filterByCategory = FALSE OR rec.type IN :categories)
              AND (:filterByImportance = FALSE OR rec.priory IN :priorities)

            ORDER BY ecnmc_indcr_prsnttn_day ASC, ecnmc_indcr_prsnttn_tm ASC, priory DESC
            LIMIT :limit OFFSET :offset
            """,
        nativeQuery = true)
    List<Object[]> findEconomicIndicatorsByPeriodWithStatus(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("priorities") List<Short> priorities,
        @Param("filterByImportance") boolean filterByImportance,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 통합 경제지표 총 개수 조회 (페이징용)
     */
    @Query(
        value = """
            SELECT COUNT(*) FROM (
                SELECT ec.ecnmc_cal_id
                FROM tif_ecnmc_cal ec
                LEFT JOIN tif_refiecnmicclndrr rec ON ec.code = rec.code
                WHERE ec.ecnmc_indcr_prsnttn_day >= :startDate
                  AND ec.ecnmc_indcr_prsnttn_day <= :endDate
                  AND ec.del_yn = 'N'
                  AND (:keyword IS NULL OR :keyword = ''
                       OR UPPER(ec.ecnmc_indcr_nm) LIKE UPPER(CONCAT('%', :keyword, '%')))
                  AND (:filterByCategory = FALSE OR rec.type IN :categories)
                  AND (:filterByImportance = FALSE OR rec.priory IN :priorities)
                UNION ALL
                SELECT ec.ecnmc_cal_id
                FROM tif_ecnmc_cal ec
                LEFT JOIN tif_refiecnmicclndrr rec ON ec.code = rec.code
                WHERE ec.nxt_ecnmc_indcr_prsnttn_day >= :startDate
                  AND ec.nxt_ecnmc_indcr_prsnttn_day <= :endDate
                  AND ec.del_yn = 'N'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM tif_ecnmc_cal announced
                      WHERE announced.code = ec.code
                        AND announced.ecnmc_indcr_prsnttn_day = ec.nxt_ecnmc_indcr_prsnttn_day
                        AND announced.ecnmc_indcr_prsnttn_day >= :startDate
                        AND announced.ecnmc_indcr_prsnttn_day <= :endDate
                        AND announced.del_yn = 'N'
                  )
                  AND (:keyword IS NULL OR :keyword = ''
                       OR UPPER(ec.ecnmc_indcr_nm) LIKE UPPER(CONCAT('%', :keyword, '%')))
                  AND (:filterByCategory = FALSE OR rec.type IN :categories)
                  AND (:filterByImportance = FALSE OR rec.priory IN :priorities)
            ) AS total
            """,
        nativeQuery = true)
    long countEconomicIndicatorsByPeriodWithStatus(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("priorities") List<Short> priorities,
        @Param("filterByImportance") boolean filterByImportance
    );

    @Query(
        value = """
            select new com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarDetailRes(
                ec.economicCalendarId,
                ec.code,
                ec.date,
                ec.economicIndicatorName,
                ec.economicIndicatorPresentationDay,
                ec.economicIndicatorPresentationTime,
                ec.economicIndicatorRealValue,
                ec.economicIndicatorPredictionDay,
                ec.economicIndicatorPresentationPriorValue,
                ec.economicIndicatorPredictionValue,
                ec.unitPrefix,
                ec.unit,
                ec.nextEconomicIndicatorPresentationDay,
                rec.name,
                rec.presentationRegion,
                rec.explanation,
                rec.sourceOrganization,
                rec.type,
                rec.unit,
                rec.presentationCycle,
                rec.isUsed,
                rec.priority,
                rec.presentationDatetime,
                null,
                null
            )
            from EconomicCalendar ec
            left join RefinitivEconomicCalendar rec on ec.code = rec.code
            where ec.code = :code
              and ec.isDeleted = false
            order by ec.date desc
            limit 1
            """
    )
    Optional<EconomicCalendarDetailRes> findLatestEconomicCalendarByCode(
        @Param("code") String code
    );

    /**
     * 경제 지표 달력 히스토리 조회 (code 기준, 최신순)
     */
    Page<EconomicCalendar> findByCodeAndIsDeletedFalseOrderByDateDesc(String code, Pageable pageable);

    List<EconomicCalendar> findByEconomicCalendarIdIn(List<Long> economicCalendarIds);
}

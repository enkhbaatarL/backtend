package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarDetailRes;
import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes;
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
            select new com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes(
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
            where ec.economicIndicatorPresentationDay = :economicIndicatorPresentationDay
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """,
        countQuery = """
            select count(ec)
            from EconomicCalendar ec
            left join RefinitivEconomicCalendar rec on ec.code = rec.code
            where ec.economicIndicatorPresentationDay = :economicIndicatorPresentationDay
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """
    )
    Page<EconomicCalendarListRes> findEconomicCalendarsWithRefinitiv(
        @Param("economicIndicatorPresentationDay") String economicIndicatorPresentationDay,
        @Param("keyword") String keyword,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("priorities") List<Short> priorities,
        @Param("filterByImportance") boolean filterByImportance,
        Pageable pageable
    );

    @Query(
        value = """
            select new com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes(
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
            where ec.economicIndicatorPresentationDay >= :startDate
              and ec.economicIndicatorPresentationDay <= :endDate
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """,
        countQuery = """
            select count(ec)
            from EconomicCalendar ec
            left join RefinitivEconomicCalendar rec on ec.code = rec.code
            where ec.economicIndicatorPresentationDay >= :startDate
              and ec.economicIndicatorPresentationDay <= :endDate
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """
    )
    Page<EconomicCalendarListRes> findEconomicCalendarsWithRefinitivByDateRange(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("priorities") List<Short> priorities,
        @Param("filterByImportance") boolean filterByImportance,
        Pageable pageable
    );

    @Query(
        value = """
            select new com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes(
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
            where ec.nextEconomicIndicatorPresentationDay = :nextEconomicIndicatorPresentationDay
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """,
        countQuery = """
            select count(ec)
            from EconomicCalendar ec
            left join RefinitivEconomicCalendar rec on ec.code = rec.code
            where ec.nextEconomicIndicatorPresentationDay = :nextEconomicIndicatorPresentationDay
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """
    )
    Page<EconomicCalendarListRes> findEconomicCalendarsWithRefinitivByNextDay(
        @Param("nextEconomicIndicatorPresentationDay") String nextEconomicIndicatorPresentationDay,
        @Param("keyword") String keyword,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("priorities") List<Short> priorities,
        @Param("filterByImportance") boolean filterByImportance,
        Pageable pageable
    );

    @Query(
        value = """
            select new com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes(
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
            where ec.nextEconomicIndicatorPresentationDay >= :startDate
              and ec.nextEconomicIndicatorPresentationDay <= :endDate
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """,
        countQuery = """
            select count(ec)
            from EconomicCalendar ec
            left join RefinitivEconomicCalendar rec on ec.code = rec.code
            where ec.nextEconomicIndicatorPresentationDay >= :startDate
              and ec.nextEconomicIndicatorPresentationDay <= :endDate
              and ec.isDeleted = false
              and (:keyword is null or :keyword = '' or LOWER(ec.economicIndicatorName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              and (:filterByCategory = false or rec.type in :categories)
              and (:filterByImportance = false or rec.priority in :priorities)
            """
    )
    Page<EconomicCalendarListRes> findEconomicCalendarsWithRefinitivByNextDayRange(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        @Param("categories") List<String> categories,
        @Param("filterByCategory") boolean filterByCategory,
        @Param("priorities") List<Short> priorities,
        @Param("filterByImportance") boolean filterByImportance,
        Pageable pageable
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
}

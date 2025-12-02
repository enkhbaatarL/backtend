package com.sk.domain.shared.repository.master.tif;

import com.sk.domain.mi.dto.response.economiccalendar.EconomicCalendarListRes;
import com.sk.domain.shared.entity.master.tif.EconomicCalendar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EconomicCalendarRepository extends JpaRepository<EconomicCalendar, Long> {
    Page<EconomicCalendar> findByEconomicIndicatorPresentationDayAndIsDeletedFalse(String economicIndicatorPresentationDay, Pageable pageable);

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
            """,
        countQuery = """
            select count(ec)
            from EconomicCalendar ec
            where ec.economicIndicatorPresentationDay = :economicIndicatorPresentationDay
              and ec.isDeleted = false
            """
    )
    Page<EconomicCalendarListRes> findEconomicCalendarsWithRefinitiv(
        @Param("economicIndicatorPresentationDay") String economicIndicatorPresentationDay,
        Pageable pageable
    );
}

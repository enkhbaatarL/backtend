package com.sk.domain.shared.repository.master.tlm;

import com.sk.domain.mi.dto.response.economiccalendar.RecentViewedEconomicCalendarRes;
import com.sk.domain.shared.entity.compositeKey.tlm.EconomicCalendarInquiryLogId;
import com.sk.domain.shared.entity.master.tlm.EconomicCalendarInquiryLog;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;


public interface EconomicCalendarInquiryLogRepository
    extends JpaRepository<EconomicCalendarInquiryLog, EconomicCalendarInquiryLogId> {

    @Query("""
        SELECT new com.sk.domain.mi.dto.response.economiccalendar.RecentViewedEconomicCalendarRes(
            l.economicCalendarCode,
            r.name,
            l.inquiryDateTime,
            r.sourceOrganization
        )
        FROM EconomicCalendarInquiryLog l
        JOIN RefinitivEconomicCalendar r ON l.economicCalendarCode = r.code
        WHERE l.userNo = :userNo
          AND l.isDeleted = false
        ORDER BY l.inquiryDateTime DESC
        """)
    List<RecentViewedEconomicCalendarRes> findTop5RecentViewedWithSource(
        @Param("userNo") Integer userNo, Pageable pageable);


    @Modifying
    @Query("UPDATE EconomicCalendarInquiryLog l SET l.isDeleted = true, l.modifyDt = CURRENT_TIMESTAMP " +
           "WHERE l.userNo = :userNo AND l.isDeleted = false")
    void softDeleteAllByUserNo(@Param("userNo") int userNo);
}

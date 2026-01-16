package com.sk.domain.shared.repository.master.tlm;

import com.sk.domain.mi.dto.response.economiccalendar.RecentViewedEconomicCalendarProjection;
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

    @Query(
        value = """
            WITH ranked AS (
                SELECT
                    l.ecn_cal_code AS code,
                    MAX(l.inqr_dtm) AS max_inqr
                FROM tlm_ecncalinqrusrl l
                WHERE l.usr_no = :userNo
                  AND l.del_yn = 'N'
                GROUP BY l.ecn_cal_code
                ORDER BY max_inqr DESC
                LIMIT 100
            )
            SELECT
                r.code AS code,
                rec.name AS economicIndicatorName,
                r.max_inqr AS inquiryDateTime,
                rec.src_org AS refinitivSourceOrganization
            FROM ranked r
            JOIN tif_refiecnmicclndrr rec ON rec.code = r.code
            ORDER BY r.max_inqr DESC
            """,
        nativeQuery = true
    )
    List<RecentViewedEconomicCalendarProjection> findTop5RecentViewedWithSource(
        @Param("userNo") Integer userNo, Pageable pageable);


    @Modifying
    @Query("UPDATE EconomicCalendarInquiryLog l SET l.isDeleted = true, l.modifyDt = CURRENT_TIMESTAMP " +
           "WHERE l.userNo = :userNo AND l.isDeleted = false")
    void softDeleteAllByUserNo(@Param("userNo") int userNo);
}

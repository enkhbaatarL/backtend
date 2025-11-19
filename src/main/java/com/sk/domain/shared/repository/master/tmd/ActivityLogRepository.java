package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ActivityLog;
import com.sk.domain.shared.enums.ActivityLogType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("""
        SELECT al
        FROM ActivityLog al
        LEFT JOIN FETCH al.createUser
        WHERE al.customerCompanyNo = :customerCompanyNo
            AND al.isDeleted = false
        AND (:keyword IS NULL OR :keyword = ''
          OR UPPER(al.evntDtlExpn) LIKE UPPER(CONCAT('%', :keyword, '%')))
            AND (:eventTypes IS NULL OR al.evntType IN :eventTypes)
        ORDER BY
            CASE WHEN :evntDtSort = 'ASC' THEN al.evntDt END ASC,
            CASE WHEN :evntDtSort = 'DESC' THEN al.evntDt END DESC,
            CASE WHEN :sigLvlSort = 'ASC' THEN al.sigLvl END ASC,
            CASE WHEN :sigLvlSort = 'DESC' THEN al.sigLvl END DESC,
            al.actLogId DESC
        """)
    Page<ActivityLog> findByCustomerCompanyNoAndEventTypes(
        @Param("customerCompanyNo") Integer customerCompanyNo,
        @Param("keyword") String keyword,
        @Param("eventTypes") List<ActivityLogType> eventTypes,
        @Param("evntDtSort") String evntDtSort,
        @Param("sigLvlSort") String sigLvlSort,
        Pageable pageable
    );

}

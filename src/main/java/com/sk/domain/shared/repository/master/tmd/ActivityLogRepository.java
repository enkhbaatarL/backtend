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

    /**
     * 고객회사번호와 이벤트타입 리스트로 ActivityLog 조회 (페이징)
     * User 정보를 fetch join으로 함께 조회하여 N+1 문제 방지
     */
    @Query("""
        SELECT al
        FROM ActivityLog al
        LEFT JOIN FETCH al.createUser
        WHERE al.customerCompanyNo = :customerCompanyNo
            AND al.isDeleted = false
        AND (:keyword IS NULL OR :keyword = ''
          OR UPPER(al.evntDtlExpn) LIKE UPPER(CONCAT('%', :keyword, '%')))
            AND (:eventTypes IS NULL OR al.evntType IN :eventTypes)
        """)
    Page<ActivityLog> findByCustomerCompanyNoAndEventTypes(
            @Param("customerCompanyNo") Integer customerCompanyNo,
            @Param("keyword") String keyword,
            @Param("eventTypes") List<ActivityLogType> eventTypes,
            Pageable pageable
    );

}

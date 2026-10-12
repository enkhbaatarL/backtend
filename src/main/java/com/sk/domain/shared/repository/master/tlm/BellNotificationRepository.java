package com.sk.domain.shared.repository.master.tlm;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.repository.master.tlm.projection.NotificationKeyRow;

/**
 * 알림벨 통합 키 조회
 * - 상품 임계치 알림(tlm_emlsendl)과 이벤트 키워드 알림(tmd_mi_evnt_keywdnotil)을 UNION ALL로 합쳐 DB에서 정렬·페이징한다
 * - 이벤트 키워드 분기는 includeEventKeyword(SKGAS 여부)가 false면 0건이 된다
 * - 정렬: 발생일시 내림차순, 동일 시각은 유형·ID로 순서 고정 (페이지 간 중복·누락 방지)
 * - 상세는 키를 받아 유형별 레포지토리가 조회한다
 */
public interface BellNotificationRepository extends Repository<EmailAccessLog, Integer> {

    @Query(value = """
        SELECT t.noti_type AS notificationType, t.noti_id AS notificationId
          FROM (
                SELECT 'GOODS_THRESHOLD' AS noti_type, e.eml_send_sn AS noti_id, e.eml_send_dtm AS noti_dtm
                  FROM tlm_emlsendl e
                 WHERE e.del_yn = 'N'
                   AND e.eml_rcv_usr_logn_id = :loginUserId
                   AND e.eml_send_dtm >= :since
                UNION ALL
                SELECT 'EVENT_KEYWORD', n.keywd_noti_id, n.crt_dtm
                  FROM tmd_mi_evnt_keywdnotil n
                 WHERE :includeEventKeyword = TRUE
                   AND n.del_yn = 'N'
                   AND n.usr_no = :userNo
                   AND n.crt_dtm >= :since
               ) t
         ORDER BY t.noti_dtm DESC, t.noti_type ASC, t.noti_id DESC
        """,
        countQuery = """
        SELECT (SELECT COUNT(*)
                  FROM tlm_emlsendl e
                 WHERE e.del_yn = 'N'
                   AND e.eml_rcv_usr_logn_id = :loginUserId
                   AND e.eml_send_dtm >= :since)
             + (SELECT COUNT(*)
                  FROM tmd_mi_evnt_keywdnotil n
                 WHERE :includeEventKeyword = TRUE
                   AND n.del_yn = 'N'
                   AND n.usr_no = :userNo
                   AND n.crt_dtm >= :since)
        """,
        nativeQuery = true)
    Page<NotificationKeyRow> findBellNotificationKeys(
        @Param("loginUserId") String loginUserId,
        @Param("userNo") Integer userNo,
        @Param("includeEventKeyword") boolean includeEventKeyword,
        @Param("since") LocalDateTime since,
        Pageable pageable
    );

    @Query(value = """
        SELECT (SELECT COUNT(*)
                  FROM tlm_emlsendl e
                 WHERE e.del_yn = 'N'
                   AND e.eml_inqr_yn = 'N'
                   AND e.eml_rcv_usr_logn_id = :loginUserId
                   AND e.eml_send_dtm >= :since)
             + (SELECT COUNT(*)
                  FROM tmd_mi_evnt_keywdnotil n
                 WHERE :includeEventKeyword = TRUE
                   AND n.del_yn = 'N'
                   AND n.inqr_yn = 'N'
                   AND n.usr_no = :userNo
                   AND n.crt_dtm >= :since)
        """,
        nativeQuery = true)
    long countBellNotificationUnread(
        @Param("loginUserId") String loginUserId,
        @Param("userNo") Integer userNo,
        @Param("includeEventKeyword") boolean includeEventKeyword,
        @Param("since") LocalDateTime since
    );

}

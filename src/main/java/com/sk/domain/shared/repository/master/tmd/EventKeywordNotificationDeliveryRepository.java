package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotificationDelivery;
import com.sk.domain.shared.enums.notification.NotificationChannel;
import com.sk.domain.shared.enums.notification.NotificationSendStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventKeywordNotificationDeliveryRepository extends JpaRepository<EventKeywordNotificationDelivery, Long> {

    /**
     * 발송 대상 ID: PENDING 또는 (최대 시도 수 미만 + 재시도 대기 시간이 지난) FAIL (오래된 순)
     */
    @Query("""
        SELECT d.keywordSendId
          FROM EventKeywordNotificationDelivery d
         WHERE d.channel = :channel
           AND d.isDeleted = false
           AND (d.status = :pending OR (d.status = :fail AND d.sendTryCount < :maxAttempts AND d.sendDtm < :retryBefore))
         ORDER BY d.keywordSendId ASC
        """)
    List<Long> findDispatchableIds(
            @Param("channel") NotificationChannel channel,
            @Param("pending") NotificationSendStatus pending,
            @Param("fail") NotificationSendStatus fail,
            @Param("maxAttempts") int maxAttempts,
            @Param("retryBefore") LocalDateTime retryBefore,
            Pageable pageable
    );

    /**
     * 발송 선점 (조건부 UPDATE). 여러 노드가 같은 행을 동시에 시도해도 1건 갱신은 한 노드만 받는다
     *
     * @return 1이면 선점 성공, 0이면 다른 노드가 먼저 선점했거나 대상 상태가 아님
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE EventKeywordNotificationDelivery d
           SET d.status = :sending, d.modifyDt = :now
         WHERE d.keywordSendId = :keywordSendId
           AND (d.status = :pending OR (d.status = :fail AND d.sendTryCount < :maxAttempts AND d.sendDtm < :retryBefore))
        """)
    int claim(
            @Param("keywordSendId") Long keywordSendId,
            @Param("sending") NotificationSendStatus sending,
            @Param("pending") NotificationSendStatus pending,
            @Param("fail") NotificationSendStatus fail,
            @Param("maxAttempts") int maxAttempts,
            @Param("retryBefore") LocalDateTime retryBefore,
            @Param("now") LocalDateTime now
    );

    /**
     * SENDING 타임아웃 복구: 선점 후 결과를 남기지 못한 행(노드 중단 등)을 재시도 없는 FAIL로 바꾼다
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE EventKeywordNotificationDelivery d
           SET d.status = :fail, d.sendTryCount = :maxAttempts, d.errorMessage = :errorMessage, d.modifyDt = :now
         WHERE d.channel = :channel
           AND d.status = :sending
           AND d.modifyDt < :sendingBefore
        """)
    int failStuckSending(
            @Param("channel") NotificationChannel channel,
            @Param("sending") NotificationSendStatus sending,
            @Param("fail") NotificationSendStatus fail,
            @Param("maxAttempts") int maxAttempts,
            @Param("errorMessage") String errorMessage,
            @Param("sendingBefore") LocalDateTime sendingBefore,
            @Param("now") LocalDateTime now
    );

    /**
     * 발송 내용 조립용 조회 (알림, 수신 사용자까지 함께 로딩)
     */
    @Query("""
        SELECT d
          FROM EventKeywordNotificationDelivery d
          JOIN FETCH d.notification n
          JOIN FETCH n.user u
         WHERE d.keywordSendId = :keywordSendId
        """)
    Optional<EventKeywordNotificationDelivery> findWithNotificationAndUser(@Param("keywordSendId") Long keywordSendId);
}

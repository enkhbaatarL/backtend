package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventKeywordNotificationRepository extends JpaRepository<EventKeywordNotification, Long> {

    /**
     * 이벤트에 이미 생성된 알림의 키워드 설정 ID (재스캔 시 중복 생성 방지)
     */
    @Query("""
        SELECT n.keywordSetting.keywordSettingId
          FROM EventKeywordNotification n
         WHERE n.miEventId = :miEventId
        """)
    Set<Long> findKeywordSettingIdsByMiEventId(@Param("miEventId") Long miEventId);

    /** 알림벨·My Notifications 상세 조회 — 본인 + 미삭제 (키워드 설정 함께 로딩) */
    @Query("""
        SELECT n
          FROM EventKeywordNotification n
          JOIN FETCH n.keywordSetting
         WHERE n.keywordNotificationId IN :ids
           AND n.user.userNo = :userNo
           AND n.isDeleted = false
        """)
    List<EventKeywordNotification> findAllWithKeywordSetting(@Param("ids") Collection<Long> ids, @Param("userNo") Integer userNo);

    /** 읽음 처리 대상 — 본인 + 미삭제 */
    Optional<EventKeywordNotification> findByKeywordNotificationIdAndUser_UserNoAndIsDeletedFalse(Long keywordNotificationId, Integer userNo);
}

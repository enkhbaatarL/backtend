package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;
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
}

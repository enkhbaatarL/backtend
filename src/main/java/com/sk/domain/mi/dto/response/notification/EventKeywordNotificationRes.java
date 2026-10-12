package com.sk.domain.mi.dto.response.notification;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이벤트 키워드 알림 상세 (알림벨·My Notifications 항목의 유형별 객체)
 */
public record EventKeywordNotificationRes(

        @Schema(description = "키워드 설정 ID", example = "31")
        Long keywordSettingId,

        @Schema(description = "키워드명 (매칭 시점 스냅샷)", example = "카타르")
        String keywordName,

        @Schema(description = "이벤트 ID", example = "1234")
        Long eventId,

        @Schema(description = "이벤트명 (매칭 시점 스냅샷)", example = "[카타르 LNG 공급 제약] 불가항력 9월 말까지 연장")
        String eventName

) {

    public static EventKeywordNotificationRes of(EventKeywordNotification notification) {
        return new EventKeywordNotificationRes(
                notification.getKeywordSetting().getKeywordSettingId(),
                notification.getKeywordSnapshotName(),
                notification.getMiEventId(),
                notification.getEventSnapshotName());
    }
}

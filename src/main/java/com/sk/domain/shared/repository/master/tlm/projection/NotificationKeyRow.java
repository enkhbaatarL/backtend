package com.sk.domain.shared.repository.master.tlm.projection;

/**
 * 알림 키 (유형 + 원천 ID) — 알림벨·My Notifications 공통 — UNION ALL 정렬·페이징 결과
 */
public interface NotificationKeyRow {

    /** NotificationSourceType 이름 (GOODS_THRESHOLD / EVENT_KEYWORD) */
    String getNotificationType();

    Long getNotificationId();

}

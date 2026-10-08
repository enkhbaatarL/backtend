package com.sk.domain.shared.enums.notification;

/**
 * 알림 발송 상태 (send_stat_cd)
 * - PENDING → SENDING(디스패처가 조건부 UPDATE로 선점) → SUCCESS / FAIL
 * - FAIL은 최대 시도 수 미만이면 다시 선점 대상이 된다
 */
public enum NotificationSendStatus {
    PENDING,
    SENDING,
    SUCCESS,
    FAIL
}

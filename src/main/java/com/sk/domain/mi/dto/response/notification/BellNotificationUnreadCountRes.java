package com.sk.domain.mi.dto.response.notification;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림벨 미조회 수
 */
public record BellNotificationUnreadCountRes(

    @Schema(description = "미조회 알림 수 (최근 7일)", example = "3")
    long unreadCount

) {
}

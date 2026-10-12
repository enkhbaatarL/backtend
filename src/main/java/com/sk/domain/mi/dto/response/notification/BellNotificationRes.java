package com.sk.domain.mi.dto.response.notification;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;
import com.sk.domain.shared.enums.notification.NotificationSourceType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림벨 항목 — 공통 필드 + 유형별 객체 (해당 유형 객체만 채우고 나머지는 null)
 */
public record BellNotificationRes(

        @Schema(description = "알림 유형 (GOODS_THRESHOLD: 상품 임계치, EVENT_KEYWORD: 이벤트 키워드)", example = "EVENT_KEYWORD")
        NotificationSourceType notificationType,

        @Schema(description = "알림 ID (유형별 원천 ID — 상품: emailSendSn, 키워드: 키워드 알림 ID)", example = "1234")
        Long notificationId,

        @Schema(description = "알림 발생 일시 (목록 정렬 기준)", example = "2026-10-08 10:00:40")
        LocalDateTime notifiedAt,

        @Schema(description = "읽음 여부", example = "false")
        Boolean isRead,

        @Schema(description = "상품 임계치 알림 (기존 알림 발송 내역 응답과 동일). 그 외 유형은 null", nullable = true)
        MiNotificationRes goodsThreshold,

        @Schema(description = "이벤트 키워드 알림. 그 외 유형은 null", nullable = true)
        EventKeywordNotificationRes eventKeyword

) {

    public static BellNotificationRes ofGoodsThreshold(MiNotificationRes goodsThreshold) {
        return new BellNotificationRes(
                NotificationSourceType.GOODS_THRESHOLD,
                goodsThreshold.emailSendSn().longValue(),
                goodsThreshold.emailSendDtm(),
                goodsThreshold.isEmailInquiry(),
                goodsThreshold,
                null);
    }

    public static BellNotificationRes ofEventKeyword(EventKeywordNotification notification) {
        return new BellNotificationRes(
                NotificationSourceType.EVENT_KEYWORD,
                notification.getKeywordNotificationId(),
                notification.getCreateDt(),
                notification.getIsInquiry(),
                null,
                EventKeywordNotificationRes.of(notification));
    }
}

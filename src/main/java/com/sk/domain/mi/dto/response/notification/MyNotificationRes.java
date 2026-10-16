package com.sk.domain.mi.dto.response.notification;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;
import com.sk.domain.shared.enums.notification.NotificationSourceType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * My Notifications 항목 — 유형 + 유형별 객체 (해당 유형 객체만 채우고 나머지는 null)
 */
public record MyNotificationRes(

        @Schema(description = "알림 유형 (GOODS_THRESHOLD: 상품 임계치, EVENT_KEYWORD: 이벤트 키워드)", example = "EVENT_KEYWORD")
        NotificationSourceType notificationType,

        @Schema(description = "상품 임계치 알림 설정 (기존 알림 설정한 정형 데이터 목록 응답과 동일). 그 외 유형은 null", nullable = true)
        MiNotificationSettingGoodsDataSourceRes goodsThreshold,

        @Schema(description = "이벤트 키워드 알림. 그 외 유형은 null", nullable = true)
        EventKeywordNotificationRes eventKeyword

) {

    public static MyNotificationRes ofGoodsThreshold(MiNotificationSettingGoodsDataSourceRes goodsThreshold) {
        return new MyNotificationRes(NotificationSourceType.GOODS_THRESHOLD, goodsThreshold, null);
    }

    public static MyNotificationRes ofEventKeyword(EventKeywordNotification notification) {
        return new MyNotificationRes(NotificationSourceType.EVENT_KEYWORD, null, EventKeywordNotificationRes.of(notification));
    }

}

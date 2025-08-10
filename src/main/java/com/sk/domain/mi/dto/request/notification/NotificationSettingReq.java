package com.sk.domain.mi.dto.request.notification;

import java.math.BigDecimal;

import com.sk.domain.shared.enums.NotiCrtvlSptCd;
import com.sk.domain.shared.enums.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;


public record NotificationSettingReq() {
    // 중첩 레코드: NotificationSetting
    public record Insert(
        @NotNull
        @Schema(description = "알림 임계치 구분 코드", example = "UPPER or LOWER or INCRATE or DECRATE")
        NotiCrtvlSptCd notificationCriticalValueSeparateCd,

        @DecimalMin(value = "0.0", message = "알림 임계치는 0.0 이상이어야 합니다.")
        @Schema(description = "알림 임계치", example = "100.0")
        BigDecimal criticalValueSettingVol,

        @NotNull
        @Schema(description = "알림 타입 ACTUAL: 실제 or FORECAST: 예측", example = "FORECAST", allowableValues = {"FORECAST", "ACTUAL"})
        NotificationType notificationType
    ) {}

    public record UpdateSetting(
        @Schema(description = "사용자상품알림설정번호", example = "1")
        Integer userGoodsNotificationSettingNo,

        @NotNull
        @Schema(description = "알림 임계치 구분 코드", example = "UPPER or LOWER or INCRATE or DECRATE")
        NotiCrtvlSptCd notificationCriticalValueSeparateCd,

        @DecimalMin(value = "0.0", message = "알림 임계치는 0.0 이상이어야 합니다.")
        @Schema(description = "알림 임계치", example = "100.0")
        BigDecimal criticalValueSettingVol

    ) {}
    public record UpdateUserSetting(
        @NotNull
        @Schema(description = "사용자알림설정여부", example = "true or false")
        Boolean isUserNotificationSetting
    ) {}
}
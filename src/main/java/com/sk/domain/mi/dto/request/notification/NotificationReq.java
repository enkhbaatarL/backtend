package com.sk.domain.mi.dto.request.notification;

import java.math.BigDecimal;

import com.sk.domain.shared.enums.NotiCrtvlSptCd;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


public record NotificationReq() {
    // 중첩 레코드: NotificationSetting
    public record Insert(
        @NotNull
        @Schema(name = "notiCrtvlSptCd", description = "알림 임계치 구분 코드", example = "UPPER / LOWER / INCRATE / DECRATE")
        NotiCrtvlSptCd notificationCriticalValueSeparateCd,

        @DecimalMin(value = "0.0", message = "알림 임계치는 0.0 이상이어야 합니다.")
        @Schema(name = "crtvlSetVol", description = "알림 임계치", example = "100.0")
        BigDecimal criticalValueSettingVol,

        @Schema(name = "crtvlSetUnitNm", description = "알림 임계치 단위명", example = "USD")
        String criticalValueSettingUnitNm
    ) {}

    public record UpdateSetting(
        @Schema(name = "userGoodsNotificationSettingNo", description = "사용자상품알림설정번호", example = "1")
        Integer userGoodsNotificationSettingNo,

        @NotNull
        @Schema(name = "notiCrtvlSptCd", description = "알림 임계치 구분 코드", example = "UPPER / LOWER / INCRATE / DECRATE")
        NotiCrtvlSptCd notificationCriticalValueSeparateCd,

        @DecimalMin(value = "0.0", message = "알림 임계치는 0.0 이상이어야 합니다.")
        @Schema(name = "crtvlSetVol", description = "알림 임계치", example = "100.0")
        BigDecimal criticalValueSettingVol,

        @Schema(name = "crtvlSetUnitNm", description = "알림 임계치 단위명", example = "USD")
        String criticalValueSettingUnitNm
    ) {}
}
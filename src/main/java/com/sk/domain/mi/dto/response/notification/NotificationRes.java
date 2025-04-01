package com.sk.domain.mi.dto.response.notification;

import java.math.BigDecimal;

import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.enums.NotiCrtvlSptCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record NotificationRes(
    @Schema(description = "알림번호", example = "1")
    Integer userGoodsNotificationSettingNo,

    @Schema(description = "사용자번호", example = "1")
    Integer userNo,

    @Schema(description = "상품데이터소스번호", example = "1")
    Integer goodsDataSourceNo,

    @Schema(description = "알림임계치구분코드", example = "UPPER")
    NotiCrtvlSptCd notificationCriticalValueSeparateCd,

    @Schema(description = "임계치설정량", example = "100")
    BigDecimal criticalValueSettingVol,

    @Schema(description = "임계치설정단위명", example = "kg")
    String criticalValueSettingUnitNm
) {

    @Builder
    public NotificationRes(Integer userGoodsNotificationSettingNo, Integer userNo, Integer goodsDataSourceNo,
            NotiCrtvlSptCd notificationCriticalValueSeparateCd, BigDecimal criticalValueSettingVol,
            String criticalValueSettingUnitNm) {
        this.userGoodsNotificationSettingNo = userGoodsNotificationSettingNo;
        this.userNo = userNo;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.notificationCriticalValueSeparateCd = notificationCriticalValueSeparateCd;
        this.criticalValueSettingVol = criticalValueSettingVol;
        this.criticalValueSettingUnitNm = criticalValueSettingUnitNm;
    }

    public static NotificationRes of(GoodsCriticalValueSetting dto) {
        return NotificationRes.builder()
            .userGoodsNotificationSettingNo(dto.getUserGoodsNotificationSettingNo())
            .userNo(dto.getUser().getUserNo())
            .goodsDataSourceNo(dto.getGoodsDataSourceDetail().getGoodsDataSourceNo())
            .notificationCriticalValueSeparateCd(dto.getNotificationCriticalValueSeparateCd())
            .criticalValueSettingVol(dto.getCriticalValueSettingVol())
            .criticalValueSettingUnitNm(dto.getCriticalValueSettingUnitNm())
            .build();
    }
}

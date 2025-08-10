package com.sk.domain.mi.dto.response.notification;

import java.math.BigDecimal;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tmd.Goods;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.NotiCrtvlSptCd;
import com.sk.domain.shared.enums.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiNotificationSettingRes(
        @Schema(description = "사용자상품알림설정번호", example = "1") Integer userGoodsNotificationSettingNo,

        @Schema(description = "사용자번호", example = "1") Integer userNo,

        @Schema(description = "상품번호", example = "1") Integer goodsNo,

        @Schema(description = "상품데이터번호", example = "1") Integer goodsDataNo,

        @Schema(description = "상품데이터소스번호", example = "1") Integer goodsDataSourceNo,

        @Schema(description = "상품데이터소스명", example = "상품데이터소스명") String goodsDataSourceNm,

        @Schema(description = "알림임계치구분코드", example = "UPPER / LOWER / INCRATE / DECRATE") NotiCrtvlSptCd notificationCriticalValueSeparateCd,

        @Schema(description = "임계치설정량", example = "100") BigDecimal criticalValueSettingVol,

        @Schema(description = "임계치설정단위명", example = "kg / %") String criticalValueSettingUnitNm,

        @Schema(description = "알림 타입 ACTUAL: 실제 or FORECAST: 예측", example = "ACTUAL") NotificationType notificationType) {

    @Builder
    public MiNotificationSettingRes(Integer userGoodsNotificationSettingNo, Integer userNo, Integer goodsNo, Integer goodsDataNo,
            Integer goodsDataSourceNo, String goodsDataSourceNm, NotiCrtvlSptCd notificationCriticalValueSeparateCd,
            BigDecimal criticalValueSettingVol, String criticalValueSettingUnitNm, NotificationType notificationType) {
        this.userGoodsNotificationSettingNo = userGoodsNotificationSettingNo;
        this.userNo = userNo;
        this.goodsNo = goodsNo;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.notificationCriticalValueSeparateCd = notificationCriticalValueSeparateCd;
        this.criticalValueSettingVol = criticalValueSettingVol;
        this.criticalValueSettingUnitNm = criticalValueSettingUnitNm;
        this.notificationType = notificationType;
    }

    @QueryProjection
    public MiNotificationSettingRes(Integer userGoodsNotificationSettingNo, Integer userNo, Integer goodsNo, Integer goodsDataNo,
            Integer goodsDataSourceNo, String goodsDataSourceNm, NotiCrtvlSptCd notificationCriticalValueSeparateCd,
            BigDecimal criticalValueSettingVol, String criticalValueSettingUnitNm, Boolean isPredictData) {
        this(userGoodsNotificationSettingNo, userNo, goodsNo, goodsDataNo, goodsDataSourceNo, goodsDataSourceNm,
                notificationCriticalValueSeparateCd, criticalValueSettingVol, criticalValueSettingUnitNm,
                isPredictData ? NotificationType.FORECAST : NotificationType.ACTUAL);
    }

    public static MiNotificationSettingRes of(GoodsCriticalValueSetting dto, GoodsDataSourceDetail goodsDataSourceDetail) {
        return MiNotificationSettingRes.builder()
                .userGoodsNotificationSettingNo(dto.getUserGoodsNotificationSettingNo())
                .userNo(dto.getUser().getUserNo())
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .goodsDataSourceNm(goodsDataSourceDetail.getGoodsDataSourceNm())
                .notificationCriticalValueSeparateCd(dto.getNotificationCriticalValueSeparateCd())
                .criticalValueSettingVol(dto.getCriticalValueSettingVol())
                .criticalValueSettingUnitNm(dto.getCriticalValueSettingUnitNm())
                .notificationType(dto.getIsPredictData() ? NotificationType.FORECAST : NotificationType.ACTUAL)
                .build();
    }

    public static MiNotificationSettingRes ofDetail(GoodsCriticalValueSetting dto, GoodsDataSourceDetail goodsDataSourceDetail, GoodsData goodsData, Goods goods) {
        return MiNotificationSettingRes.builder()
                .userGoodsNotificationSettingNo(dto.getUserGoodsNotificationSettingNo())
                .userNo(dto.getUser().getUserNo())
                .goodsNo(goods.getGoodsNo())
                .goodsDataNo(goodsData.getGoodsDataNo())
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .goodsDataSourceNm(goodsDataSourceDetail.getGoodsDataSourceNm())
                .notificationCriticalValueSeparateCd(dto.getNotificationCriticalValueSeparateCd())
                .criticalValueSettingVol(dto.getCriticalValueSettingVol())
                .criticalValueSettingUnitNm(dto.getCriticalValueSettingUnitNm())
                .notificationType(dto.getIsPredictData() ? NotificationType.FORECAST : NotificationType.ACTUAL)
                .build();
    }
}

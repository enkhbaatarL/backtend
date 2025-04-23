package com.sk.domain.mi.dto.response.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.enums.NotiCrtvlSptCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiNotificationRes(
        @Schema(description = "이메일 발송 일련번호", example = "1") Integer emailSendSn,

        @Schema(description = "이메일 제목", example = "이메일 제목") String emailTitleNm,

        @Schema(description = "이메일 내용", example = "이메일 내용") String emailCntt,

        @Schema(description = "이메일 발송 일시", example = "2021-01-01 00:00:00") LocalDateTime emailSendDtm,

        @Schema(description = "이메일 조회 여부", example = "true") Boolean isEmailInquiry,

        @Schema(description = "알림임계치구분코드", example = "UPPER or LOWER or INCRATE or DECRATE") NotiCrtvlSptCd notificationCriticalValueSeparateCd,
        
        @Schema(description = "임계치설정량", example = "1000000") BigDecimal criticalValueSettingVol,

        @Schema(description = "임계치설정단위명", example = "USD/Berrel") String criticalValueSettingUnitNm,

        @Schema(description = "정형데이터 명", example = "정형데이터 명") String goodsDataSourceNm,

        @Schema(description = "상품데이터소스번호", example = "1") Integer goodsDataSourceNo

        ) {

    @Builder
    @QueryProjection
    public MiNotificationRes(Integer emailSendSn, String emailTitleNm, String emailCntt,
            LocalDateTime emailSendDtm, Boolean isEmailInquiry, NotiCrtvlSptCd notificationCriticalValueSeparateCd,
            BigDecimal criticalValueSettingVol, String criticalValueSettingUnitNm, String goodsDataSourceNm,
            Integer goodsDataSourceNo) {
        this.emailSendSn = emailSendSn;
        this.emailTitleNm = emailTitleNm;
        this.emailCntt = emailCntt;
        this.emailSendDtm = emailSendDtm;
        this.isEmailInquiry = isEmailInquiry;
        this.notificationCriticalValueSeparateCd = notificationCriticalValueSeparateCd;
        this.criticalValueSettingVol = criticalValueSettingVol;
        this.criticalValueSettingUnitNm = criticalValueSettingUnitNm;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.goodsDataSourceNo = goodsDataSourceNo;
    }

    public static MiNotificationRes of(EmailAccessLog emailAccessLog) {
        return MiNotificationRes.builder()
                .emailSendSn(emailAccessLog.getEmailSendSn())
                .emailTitleNm(emailAccessLog.getEmailTitleNm())
                .emailCntt(emailAccessLog.getEmailCntt())
                .emailSendDtm(emailAccessLog.getEmailSendDtm())
                .isEmailInquiry(emailAccessLog.getIsEmailInquiry())
                .notificationCriticalValueSeparateCd(emailAccessLog.getGoodsCriticalValueSetting().getNotificationCriticalValueSeparateCd())
                .criticalValueSettingVol(emailAccessLog.getGoodsCriticalValueSetting().getCriticalValueSettingVol())
                .criticalValueSettingUnitNm(emailAccessLog.getGoodsCriticalValueSetting().getCriticalValueSettingUnitNm())
                .goodsDataSourceNm(emailAccessLog.getGoodsCriticalValueSetting().getGoodsDataSourceDetail().getGoodsDataSourceNm())
                .goodsDataSourceNo(emailAccessLog.getGoodsCriticalValueSetting().getGoodsDataSourceDetail().getGoodsDataSourceNo())
                .build();
    }
}

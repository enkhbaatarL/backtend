package com.sk.domain.mi.dto.response.notification;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.mi.dto.response.notification.QMiNotificationRes is a Querydsl Projection type for MiNotificationRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMiNotificationRes extends ConstructorExpression<MiNotificationRes> {

    private static final long serialVersionUID = 953536547L;

    public QMiNotificationRes(com.querydsl.core.types.Expression<Integer> emailSendSn, com.querydsl.core.types.Expression<String> emailTitleNm, com.querydsl.core.types.Expression<String> emailCntt, com.querydsl.core.types.Expression<java.time.LocalDateTime> emailSendDtm, com.querydsl.core.types.Expression<Boolean> isEmailInquiry, com.querydsl.core.types.Expression<com.sk.domain.shared.enums.NotiCrtvlSptCd> notificationCriticalValueSeparateCd, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> criticalValueSettingVol, com.querydsl.core.types.Expression<String> criticalValueSettingUnitNm, com.querydsl.core.types.Expression<String> goodsDataSourceNm, com.querydsl.core.types.Expression<Integer> goodsDataSourceNo, com.querydsl.core.types.Expression<Integer> goodsDataNo, com.querydsl.core.types.Expression<Integer> goodsNo) {
        super(MiNotificationRes.class, new Class<?>[]{int.class, String.class, String.class, java.time.LocalDateTime.class, boolean.class, com.sk.domain.shared.enums.NotiCrtvlSptCd.class, java.math.BigDecimal.class, String.class, String.class, int.class, int.class, int.class}, emailSendSn, emailTitleNm, emailCntt, emailSendDtm, isEmailInquiry, notificationCriticalValueSeparateCd, criticalValueSettingVol, criticalValueSettingUnitNm, goodsDataSourceNm, goodsDataSourceNo, goodsDataNo, goodsNo);
    }

}


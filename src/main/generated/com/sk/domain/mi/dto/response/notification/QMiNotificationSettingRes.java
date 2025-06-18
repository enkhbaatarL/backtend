package com.sk.domain.mi.dto.response.notification;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.mi.dto.response.notification.QMiNotificationSettingRes is a Querydsl Projection type for MiNotificationSettingRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMiNotificationSettingRes extends ConstructorExpression<MiNotificationSettingRes> {

    private static final long serialVersionUID = 1404161869L;

    public QMiNotificationSettingRes(com.querydsl.core.types.Expression<Integer> userGoodsNotificationSettingNo, com.querydsl.core.types.Expression<Integer> userNo, com.querydsl.core.types.Expression<Integer> goodsNo, com.querydsl.core.types.Expression<Integer> goodsDataNo, com.querydsl.core.types.Expression<Integer> goodsDataSourceNo, com.querydsl.core.types.Expression<String> goodsDataSourceNm, com.querydsl.core.types.Expression<com.sk.domain.shared.enums.NotiCrtvlSptCd> notificationCriticalValueSeparateCd, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> criticalValueSettingVol, com.querydsl.core.types.Expression<String> criticalValueSettingUnitNm) {
        super(MiNotificationSettingRes.class, new Class<?>[]{int.class, int.class, int.class, int.class, int.class, String.class, com.sk.domain.shared.enums.NotiCrtvlSptCd.class, java.math.BigDecimal.class, String.class}, userGoodsNotificationSettingNo, userNo, goodsNo, goodsDataNo, goodsDataSourceNo, goodsDataSourceNm, notificationCriticalValueSeparateCd, criticalValueSettingVol, criticalValueSettingUnitNm);
    }

}


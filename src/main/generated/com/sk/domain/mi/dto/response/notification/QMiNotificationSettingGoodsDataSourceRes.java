package com.sk.domain.mi.dto.response.notification;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.mi.dto.response.notification.QMiNotificationSettingGoodsDataSourceRes is a Querydsl Projection type for MiNotificationSettingGoodsDataSourceRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMiNotificationSettingGoodsDataSourceRes extends ConstructorExpression<MiNotificationSettingGoodsDataSourceRes> {

    private static final long serialVersionUID = -1666442248L;

    public QMiNotificationSettingGoodsDataSourceRes(com.querydsl.core.types.Expression<Integer> goodsNo, com.querydsl.core.types.Expression<Integer> goodsDataNo, com.querydsl.core.types.Expression<String> goodsDataNm, com.querydsl.core.types.Expression<Integer> goodsDataSourceNo, com.querydsl.core.types.Expression<String> goodsDataSourceNm, com.querydsl.core.types.Expression<Integer> settingNotificationCount) {
        super(MiNotificationSettingGoodsDataSourceRes.class, new Class<?>[]{int.class, int.class, String.class, int.class, String.class, int.class}, goodsNo, goodsDataNo, goodsDataNm, goodsDataSourceNo, goodsDataSourceNm, settingNotificationCount);
    }

}


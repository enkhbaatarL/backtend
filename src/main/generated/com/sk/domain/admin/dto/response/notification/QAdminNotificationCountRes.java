package com.sk.domain.admin.dto.response.notification;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.notification.QAdminNotificationCountRes is a Querydsl Projection type for AdminNotificationCountRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QAdminNotificationCountRes extends ConstructorExpression<AdminNotificationCountRes> {

    private static final long serialVersionUID = -1409751290L;

    public QAdminNotificationCountRes(com.querydsl.core.types.Expression<Integer> totalEmailSendCnt, com.querydsl.core.types.Expression<Integer> totalEmailSuccessCnt, com.querydsl.core.types.Expression<Integer> totalEmailFailCnt, com.querydsl.core.types.Expression<java.time.LocalDateTime> lastUpdatedDt) {
        super(AdminNotificationCountRes.class, new Class<?>[]{int.class, int.class, int.class, java.time.LocalDateTime.class}, totalEmailSendCnt, totalEmailSuccessCnt, totalEmailFailCnt, lastUpdatedDt);
    }

}


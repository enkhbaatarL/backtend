package com.sk.domain.admin.dto.response.notification;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.notification.QAdminNotificationRes is a Querydsl Projection type for AdminNotificationRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QAdminNotificationRes extends ConstructorExpression<AdminNotificationRes> {

    private static final long serialVersionUID = 1203457419L;

    public QAdminNotificationRes(com.querydsl.core.types.Expression<Integer> emailSendSn, com.querydsl.core.types.Expression<String> emailReceiveUserLoginId, com.querydsl.core.types.Expression<String> emailTitleNm, com.querydsl.core.types.Expression<String> emailCntt, com.querydsl.core.types.Expression<com.sk.domain.shared.enums.EmailSendStatusName> emailSendStatusNm, com.querydsl.core.types.Expression<String> emailSendErrorCntt, com.querydsl.core.types.Expression<java.time.LocalDateTime> emailSendDtm, com.querydsl.core.types.Expression<Boolean> isSendSuccess, com.querydsl.core.types.Expression<String> userNm, com.querydsl.core.types.Expression<com.sk.domain.shared.enums.UserRoleCd> userRoleCd, com.querydsl.core.types.Expression<String> positionNm, com.querydsl.core.types.Expression<String> jobGradeNm, com.querydsl.core.types.Expression<String> onDutyNm, com.querydsl.core.types.Expression<String> departmentNm, com.querydsl.core.types.Expression<Integer> customerCompanyNo, com.querydsl.core.types.Expression<String> customerCompanyNm, com.querydsl.core.types.Expression<Integer> goodsDataNo, com.querydsl.core.types.Expression<String> goodsDataNm, com.querydsl.core.types.Expression<String> createUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> createDt, com.querydsl.core.types.Expression<String> modifyUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> modifyDt) {
        super(AdminNotificationRes.class, new Class<?>[]{int.class, String.class, String.class, String.class, com.sk.domain.shared.enums.EmailSendStatusName.class, String.class, java.time.LocalDateTime.class, boolean.class, String.class, com.sk.domain.shared.enums.UserRoleCd.class, String.class, String.class, String.class, String.class, int.class, String.class, int.class, String.class, String.class, java.time.LocalDateTime.class, String.class, java.time.LocalDateTime.class}, emailSendSn, emailReceiveUserLoginId, emailTitleNm, emailCntt, emailSendStatusNm, emailSendErrorCntt, emailSendDtm, isSendSuccess, userNm, userRoleCd, positionNm, jobGradeNm, onDutyNm, departmentNm, customerCompanyNo, customerCompanyNm, goodsDataNo, goodsDataNm, createUserId, createDt, modifyUserId, modifyDt);
    }

}


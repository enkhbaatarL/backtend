package com.sk.domain.admin.dto.response.user;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.user.QUserInfoRes is a Querydsl Projection type for UserInfoRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QUserInfoRes extends ConstructorExpression<UserInfoRes> {

    private static final long serialVersionUID = 1676469474L;

    public QUserInfoRes(com.querydsl.core.types.Expression<String> imCd, com.querydsl.core.types.Expression<String> userNm, com.querydsl.core.types.Expression<String> userEnglishNm, com.querydsl.core.types.Expression<String> loginId, com.querydsl.core.types.Expression<String> emailAddress, com.querydsl.core.types.Expression<String> companyCd, com.querydsl.core.types.Expression<String> companyNm, com.querydsl.core.types.Expression<String> employeeNo, com.querydsl.core.types.Expression<String> departmentCd, com.querydsl.core.types.Expression<String> departmentNm, com.querydsl.core.types.Expression<String> positionCd, com.querydsl.core.types.Expression<String> positionNm, com.querydsl.core.types.Expression<String> jobGradeCd, com.querydsl.core.types.Expression<String> jobGradeNm, com.querydsl.core.types.Expression<String> onDutyCd, com.querydsl.core.types.Expression<String> onDutyNm, com.querydsl.core.types.Expression<Boolean> isRegistered) {
        super(UserInfoRes.class, new Class<?>[]{String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, boolean.class}, imCd, userNm, userEnglishNm, loginId, emailAddress, companyCd, companyNm, employeeNo, departmentCd, departmentNm, positionCd, positionNm, jobGradeCd, jobGradeNm, onDutyCd, onDutyNm, isRegistered);
    }

}


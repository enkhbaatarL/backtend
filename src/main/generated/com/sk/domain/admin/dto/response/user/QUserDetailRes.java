package com.sk.domain.admin.dto.response.user;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.user.QUserDetailRes is a Querydsl Projection type for UserDetailRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QUserDetailRes extends ConstructorExpression<UserDetailRes> {

    private static final long serialVersionUID = 1577104991L;

    public QUserDetailRes(com.querydsl.core.types.Expression<Integer> userNo, com.querydsl.core.types.Expression<String> userNm, com.querydsl.core.types.Expression<String> userLoginId, com.querydsl.core.types.Expression<com.sk.domain.shared.enums.UserRoleCd> userRoleCd, com.querydsl.core.types.Expression<String> positionNm, com.querydsl.core.types.Expression<String> jobGradeNm, com.querydsl.core.types.Expression<String> onDutyNm, com.querydsl.core.types.Expression<String> departmentNm, com.querydsl.core.types.Expression<Boolean> isOtherServiceAccessPossible, com.querydsl.core.types.Expression<Boolean> isIndividualInfoAgreement, com.querydsl.core.types.Expression<java.time.LocalDateTime> individualInfoAgreementDtm, com.querydsl.core.types.Expression<Integer> customerCompanyNo, com.querydsl.core.types.Expression<String> customerCompanyNm, com.querydsl.core.types.Expression<String> createUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> createDt, com.querydsl.core.types.Expression<String> modifyUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> modifyDt, com.querydsl.core.types.Expression<String> createUserNm, com.querydsl.core.types.Expression<String> createDepartmentNm, com.querydsl.core.types.Expression<String> createCompanyNm, com.querydsl.core.types.Expression<String> modifyUserNm, com.querydsl.core.types.Expression<String> modifyDepartmentNm, com.querydsl.core.types.Expression<String> modifyCompanyNm) {
        super(UserDetailRes.class, new Class<?>[]{int.class, String.class, String.class, com.sk.domain.shared.enums.UserRoleCd.class, String.class, String.class, String.class, String.class, boolean.class, boolean.class, java.time.LocalDateTime.class, int.class, String.class, String.class, java.time.LocalDateTime.class, String.class, java.time.LocalDateTime.class, String.class, String.class, String.class, String.class, String.class, String.class}, userNo, userNm, userLoginId, userRoleCd, positionNm, jobGradeNm, onDutyNm, departmentNm, isOtherServiceAccessPossible, isIndividualInfoAgreement, individualInfoAgreementDtm, customerCompanyNo, customerCompanyNm, createUserId, createDt, modifyUserId, modifyDt, createUserNm, createDepartmentNm, createCompanyNm, modifyUserNm, modifyDepartmentNm, modifyCompanyNm);
    }

}


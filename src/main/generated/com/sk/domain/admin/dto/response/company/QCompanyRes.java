package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.company.QCompanyRes is a Querydsl Projection type for CompanyRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCompanyRes extends ConstructorExpression<CompanyRes> {

    private static final long serialVersionUID = -1766755982L;

    public QCompanyRes(com.querydsl.core.types.Expression<Integer> companyNo, com.querydsl.core.types.Expression<String> companyCd, com.querydsl.core.types.Expression<String> companyNm, com.querydsl.core.types.Expression<String> usageBeginDate, com.querydsl.core.types.Expression<String> usageEndDate, com.querydsl.core.types.Expression<Boolean> isActivated, com.querydsl.core.types.Expression<String> createUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> createDt, com.querydsl.core.types.Expression<String> modifyUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> modifyDt, com.querydsl.core.types.Expression<String> createUserNm, com.querydsl.core.types.Expression<String> createUserDeptNm, com.querydsl.core.types.Expression<String> createUserCpnyNm, com.querydsl.core.types.Expression<String> modifyUserNm, com.querydsl.core.types.Expression<String> modifyUserDeptNm, com.querydsl.core.types.Expression<String> modifyUserCpnyNm) {
        super(CompanyRes.class, new Class<?>[]{int.class, String.class, String.class, String.class, String.class, boolean.class, String.class, java.time.LocalDateTime.class, String.class, java.time.LocalDateTime.class, String.class, String.class, String.class, String.class, String.class, String.class}, companyNo, companyCd, companyNm, usageBeginDate, usageEndDate, isActivated, createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm);
    }

}


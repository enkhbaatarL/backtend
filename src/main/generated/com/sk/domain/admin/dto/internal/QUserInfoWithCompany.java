package com.sk.domain.admin.dto.internal;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.internal.QUserInfoWithCompany is a Querydsl Projection type for UserInfoWithCompany
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QUserInfoWithCompany extends ConstructorExpression<UserInfoWithCompany> {

    private static final long serialVersionUID = 1615016892L;

    public QUserInfoWithCompany(com.querydsl.core.types.Expression<String> userNm, com.querydsl.core.types.Expression<String> userLoginId, com.querydsl.core.types.Expression<String> positionNm, com.querydsl.core.types.Expression<String> jobGradeNm, com.querydsl.core.types.Expression<String> onDutyNm, com.querydsl.core.types.Expression<String> departmentNm, com.querydsl.core.types.Expression<? extends com.sk.domain.shared.entity.master.tmd.CustomerCompany> customerCompany, com.querydsl.core.types.Expression<String> customerCompanyNm) {
        super(UserInfoWithCompany.class, new Class<?>[]{String.class, String.class, String.class, String.class, String.class, String.class, com.sk.domain.shared.entity.master.tmd.CustomerCompany.class, String.class}, userNm, userLoginId, positionNm, jobGradeNm, onDutyNm, departmentNm, customerCompany, customerCompanyNm);
    }

}


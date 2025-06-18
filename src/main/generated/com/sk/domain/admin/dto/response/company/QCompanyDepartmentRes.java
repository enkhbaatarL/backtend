package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.company.QCompanyDepartmentRes is a Querydsl Projection type for CompanyDepartmentRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCompanyDepartmentRes extends ConstructorExpression<CompanyDepartmentRes> {

    private static final long serialVersionUID = 89243200L;

    public QCompanyDepartmentRes(com.querydsl.core.types.Expression<Integer> companyNo, com.querydsl.core.types.Expression<String> companyCd, com.querydsl.core.types.Expression<String> companyNm, com.querydsl.core.types.Expression<? extends java.util.List<String>> departmentNm) {
        super(CompanyDepartmentRes.class, new Class<?>[]{int.class, String.class, String.class, java.util.List.class}, companyNo, companyCd, companyNm, departmentNm);
    }

}


package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.company.QCompanyTypeAndCompanyRes is a Querydsl Projection type for CompanyTypeAndCompanyRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCompanyTypeAndCompanyRes extends ConstructorExpression<CompanyTypeAndCompanyRes> {

    private static final long serialVersionUID = 91167826L;

    public QCompanyTypeAndCompanyRes(com.querydsl.core.types.Expression<com.sk.domain.shared.enums.CompanyType> companyType, com.querydsl.core.types.Expression<? extends java.util.List<CompanyTypeAndCompanyRes.Company>> companies) {
        super(CompanyTypeAndCompanyRes.class, new Class<?>[]{com.sk.domain.shared.enums.CompanyType.class, java.util.List.class}, companyType, companies);
    }

}


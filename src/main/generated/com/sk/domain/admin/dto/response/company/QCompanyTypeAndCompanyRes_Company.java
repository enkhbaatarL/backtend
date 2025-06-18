package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.company.QCompanyTypeAndCompanyRes_Company is a Querydsl Projection type for Company
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCompanyTypeAndCompanyRes_Company extends ConstructorExpression<CompanyTypeAndCompanyRes.Company> {

    private static final long serialVersionUID = 1725206209L;

    public QCompanyTypeAndCompanyRes_Company(com.querydsl.core.types.Expression<Integer> companyNo, com.querydsl.core.types.Expression<String> companyCd, com.querydsl.core.types.Expression<String> companyNm) {
        super(CompanyTypeAndCompanyRes.Company.class, new Class<?>[]{int.class, String.class, String.class}, companyNo, companyCd, companyNm);
    }

}


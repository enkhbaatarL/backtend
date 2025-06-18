package com.sk.domain.admin.dto.response.provider;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.provider.QStructuredDataRes is a Querydsl Projection type for StructuredDataRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QStructuredDataRes extends ConstructorExpression<StructuredDataRes> {

    private static final long serialVersionUID = 1234759046L;

    public QStructuredDataRes(com.querydsl.core.types.Expression<String> componentTypeNm, com.querydsl.core.types.Expression<java.time.LocalDateTime> createDt) {
        super(StructuredDataRes.class, new Class<?>[]{String.class, java.time.LocalDateTime.class}, componentTypeNm, createDt);
    }

}


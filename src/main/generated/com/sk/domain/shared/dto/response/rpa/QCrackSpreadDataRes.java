package com.sk.domain.shared.dto.response.rpa;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.shared.dto.response.rpa.QCrackSpreadDataRes is a Querydsl Projection type for CrackSpreadDataRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCrackSpreadDataRes extends ConstructorExpression<CrackSpreadDataRes> {

    private static final long serialVersionUID = -937373462L;

    public QCrackSpreadDataRes(com.querydsl.core.types.Expression<java.time.LocalDateTime> date, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> clPs, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> hoPs, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> rbSettle, com.querydsl.core.types.Expression<Integer> clVm, com.querydsl.core.types.Expression<Integer> clOi, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> clPo, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> clPh, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> clPl, com.querydsl.core.types.Expression<Integer> rbTotCntrVol, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> rbOpint1, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> rbOpenPrc, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> rbHigh1, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> rbLow1) {
        super(CrackSpreadDataRes.class, new Class<?>[]{java.time.LocalDateTime.class, java.math.BigDecimal.class, java.math.BigDecimal.class, java.math.BigDecimal.class, int.class, int.class, java.math.BigDecimal.class, java.math.BigDecimal.class, java.math.BigDecimal.class, int.class, java.math.BigDecimal.class, java.math.BigDecimal.class, java.math.BigDecimal.class, java.math.BigDecimal.class}, date, clPs, hoPs, rbSettle, clVm, clOi, clPo, clPh, clPl, rbTotCntrVol, rbOpint1, rbOpenPrc, rbHigh1, rbLow1);
    }

}


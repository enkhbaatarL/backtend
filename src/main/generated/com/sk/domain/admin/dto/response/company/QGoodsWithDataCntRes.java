package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.company.QGoodsWithDataCntRes is a Querydsl Projection type for GoodsWithDataCntRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QGoodsWithDataCntRes extends ConstructorExpression<GoodsWithDataCntRes> {

    private static final long serialVersionUID = -1507932306L;

    public QGoodsWithDataCntRes(com.querydsl.core.types.Expression<Integer> goodsNo, com.querydsl.core.types.Expression<String> goodsNm, com.querydsl.core.types.Expression<String> goodsExpn, com.querydsl.core.types.Expression<Integer> numberOfData, com.querydsl.core.types.Expression<Boolean> isUsed, com.querydsl.core.types.Expression<com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd> goodsAuthManagementSeparateCd, com.querydsl.core.types.Expression<String> createUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> createDt, com.querydsl.core.types.Expression<String> modifyUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> modifyDt, com.querydsl.core.types.Expression<String> createUserNm, com.querydsl.core.types.Expression<String> createUserDeptNm, com.querydsl.core.types.Expression<String> createUserCpnyNm, com.querydsl.core.types.Expression<String> modifyUserNm, com.querydsl.core.types.Expression<String> modifyUserDeptNm, com.querydsl.core.types.Expression<String> modifyUserCpnyNm) {
        super(GoodsWithDataCntRes.class, new Class<?>[]{int.class, String.class, String.class, int.class, boolean.class, com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd.class, String.class, java.time.LocalDateTime.class, String.class, java.time.LocalDateTime.class, String.class, String.class, String.class, String.class, String.class, String.class}, goodsNo, goodsNm, goodsExpn, numberOfData, isUsed, goodsAuthManagementSeparateCd, createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm);
    }

}


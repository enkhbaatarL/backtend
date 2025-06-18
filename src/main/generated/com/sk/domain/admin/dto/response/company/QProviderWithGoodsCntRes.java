package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.company.QProviderWithGoodsCntRes is a Querydsl Projection type for ProviderWithGoodsCntRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QProviderWithGoodsCntRes extends ConstructorExpression<ProviderWithGoodsCntRes> {

    private static final long serialVersionUID = 1462279271L;

    public QProviderWithGoodsCntRes(com.querydsl.core.types.Expression<Integer> providerNo, com.querydsl.core.types.Expression<String> providerNm, com.querydsl.core.types.Expression<Integer> numberOfGoods, com.querydsl.core.types.Expression<String> createUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> createDt, com.querydsl.core.types.Expression<String> modifyUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> modifyDt, com.querydsl.core.types.Expression<String> createUserNm, com.querydsl.core.types.Expression<String> createUserDeptNm, com.querydsl.core.types.Expression<String> createUserCpnyNm, com.querydsl.core.types.Expression<String> modifyUserNm, com.querydsl.core.types.Expression<String> modifyUserDeptNm, com.querydsl.core.types.Expression<String> modifyUserCpnyNm) {
        super(ProviderWithGoodsCntRes.class, new Class<?>[]{int.class, String.class, int.class, String.class, java.time.LocalDateTime.class, String.class, java.time.LocalDateTime.class, String.class, String.class, String.class, String.class, String.class, String.class}, providerNo, providerNm, numberOfGoods, createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm);
    }

}


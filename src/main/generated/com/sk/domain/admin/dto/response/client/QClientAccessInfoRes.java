package com.sk.domain.admin.dto.response.client;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.client.QClientAccessInfoRes is a Querydsl Projection type for ClientAccessInfoRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QClientAccessInfoRes extends ConstructorExpression<ClientAccessInfoRes> {

    private static final long serialVersionUID = 1881901214L;

    public QClientAccessInfoRes(com.querydsl.core.types.Expression<? extends com.sk.domain.shared.entity.master.tif.ClientAccessInfo> clientAccessInfo, com.querydsl.core.types.Expression<String> createUserNm, com.querydsl.core.types.Expression<String> createUserDeptNm, com.querydsl.core.types.Expression<String> createUserCpnyNm, com.querydsl.core.types.Expression<String> modifyUserNm, com.querydsl.core.types.Expression<String> modifyUserDeptNm, com.querydsl.core.types.Expression<String> modifyUserCpnyNm) {
        super(ClientAccessInfoRes.class, new Class<?>[]{com.sk.domain.shared.entity.master.tif.ClientAccessInfo.class, String.class, String.class, String.class, String.class, String.class, String.class}, clientAccessInfo, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm);
    }

}


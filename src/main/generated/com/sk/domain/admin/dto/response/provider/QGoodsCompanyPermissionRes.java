package com.sk.domain.admin.dto.response.provider;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.admin.dto.response.provider.QGoodsCompanyPermissionRes is a Querydsl Projection type for GoodsCompanyPermissionRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QGoodsCompanyPermissionRes extends ConstructorExpression<GoodsCompanyPermissionRes> {

    private static final long serialVersionUID = -198534709L;

    public QGoodsCompanyPermissionRes(com.querydsl.core.types.Expression<Integer> companyNo, com.querydsl.core.types.Expression<String> companyNm, com.querydsl.core.types.Expression<Boolean> isUsed, com.querydsl.core.types.Expression<com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd> goodsAuthManagementSeparateCd, com.querydsl.core.types.Expression<Boolean> viewExistsForCompany, com.querydsl.core.types.Expression<Long> viewCountForIndividual, com.querydsl.core.types.Expression<Boolean> downloadExistsForCompany, com.querydsl.core.types.Expression<Long> downloadCountForIndividual, com.querydsl.core.types.Expression<String> createUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> createDt, com.querydsl.core.types.Expression<String> modifyUserId, com.querydsl.core.types.Expression<java.time.LocalDateTime> modifyDt, com.querydsl.core.types.Expression<String> createUserNm, com.querydsl.core.types.Expression<String> createUserDeptNm, com.querydsl.core.types.Expression<String> createUserCpnyNm, com.querydsl.core.types.Expression<String> modifyUserNm, com.querydsl.core.types.Expression<String> modifyUserDeptNm, com.querydsl.core.types.Expression<String> modifyUserCpnyNm) {
        super(GoodsCompanyPermissionRes.class, new Class<?>[]{int.class, String.class, boolean.class, com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd.class, boolean.class, long.class, boolean.class, long.class, String.class, java.time.LocalDateTime.class, String.class, java.time.LocalDateTime.class, String.class, String.class, String.class, String.class, String.class, String.class}, companyNo, companyNm, isUsed, goodsAuthManagementSeparateCd, viewExistsForCompany, viewCountForIndividual, downloadExistsForCompany, downloadCountForIndividual, createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm);
    }

}


package com.sk.domain.admin.dto.response.company;

import com.sk.domain.admin.dto.request.company.AuthGoodsMapReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record AuthGoodsMapSettingRes(

        @Schema(description = "사용여부", example = "true") Boolean isUsed,

        @Schema(description = "상품권한 주체구분 코드", example = "INDIVIDUAL", allowableValues = {
                "COMPANY", "INDIVIDUAL" }) String goodsAuthManagementSeparateCd,

        @Schema(description = "상품권한 주체구분 명", example = "개인", allowableValues = { "회사", "개인" }) String goodsAuthManagementSeparateNm

    ){

    @Builder
    public AuthGoodsMapSettingRes(Boolean isUsed, String goodsAuthManagementSeparateCd, String goodsAuthManagementSeparateNm) {
        this.isUsed = isUsed;
        this.goodsAuthManagementSeparateCd = goodsAuthManagementSeparateCd;
        this.goodsAuthManagementSeparateNm = goodsAuthManagementSeparateNm;
    }

    public static AuthGoodsMapSettingRes of(AuthGoodsMapReq authGoodsMapReq) {
        return AuthGoodsMapSettingRes.builder()
                .isUsed(authGoodsMapReq.isUsed())
                .goodsAuthManagementSeparateCd(authGoodsMapReq.goodsAuthManagementSeparateCd().getCode())
                .goodsAuthManagementSeparateNm(authGoodsMapReq.goodsAuthManagementSeparateCd().getDescription())
                .build();
    }

}

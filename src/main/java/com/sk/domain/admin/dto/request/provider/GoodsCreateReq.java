package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// @formatter:off
public record GoodsCreateReq(

        @NotBlank
        @Schema(description = "상품 이름", example = "제조사별 DRAM 생산량")
        String goodsNm,

        @NotBlank
        @Schema(description = "상품ID", example = "DRAM")
        String goodsId,

        @NotBlank
        @Schema(description = "상품 설명", example = "상품 설명")
        String goodsExpn,

        @NotNull
        @Schema(description = "회사공통사용가능여부", example = "true")
        Boolean isPublic,

        @NotNull
        @Schema(description = "상품 사용유무", example = "true")
        Boolean isUsed) {

}
// @formatter:on
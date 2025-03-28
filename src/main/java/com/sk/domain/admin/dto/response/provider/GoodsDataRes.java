package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDataRes(
        @Schema(description = "상품데이터번호", example = "")
        Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "")
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "")
        String goodsDataExpn) {
}
// @formatter:on
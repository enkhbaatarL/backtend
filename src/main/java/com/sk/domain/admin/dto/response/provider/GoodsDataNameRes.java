package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDataNameRes(
        @Schema(description = "상품데이터번호", example = "1")
        Integer goodDataNo,

        @Schema(description = "상품데이터명", example = "상품데이터명")
        String goodDataNm) {
}
// @formatter:on
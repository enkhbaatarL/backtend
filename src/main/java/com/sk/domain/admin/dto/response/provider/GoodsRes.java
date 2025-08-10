package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsRes(
        @Schema(description = "상품번호", example = "5000")
        Integer goodsNo,

        @Schema(description = "타입", example = "Y")
        Boolean isPublic,

        @Schema(description = "상품ID", example = "")
        String goodsId,

        @Schema(description = "상품명", example = "제조사별 DRAM 생산량")
        String goodsNm,

        @Schema(description = "상품설명", example = "상품 설명")
        String goodsExpn,

        @Schema(description = "데이타 개수", example = "10")
        Long goodsDataSourceCount) {

}
// @formatter:on
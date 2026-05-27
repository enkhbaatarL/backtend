package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

// @formatter:off
public record GoodsDataRes(
        @Schema(description = "고객회사명", example = "")
        String customerCompanyNm,

        @Schema(description = "상품데이터번호", example = "")
        Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "")
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "")
        String goodsDataExpn,

        @Schema(description = "상품데이터 발행날짜", example = "")
        LocalDate publishedDate) {
}
// @formatter:on

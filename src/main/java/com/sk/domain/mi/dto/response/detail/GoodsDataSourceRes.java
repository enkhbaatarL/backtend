package com.sk.domain.mi.dto.response.detail;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoodsDataSourceRes(

    @Schema(description = "상품 데이터 소스 번호", example = "10004")
    Integer goodsDataSourceNo,

    @Schema(description = "상품 데이터 소스 명", example = "structured data 1234")
    String goodsDataSourceNm

) {
}

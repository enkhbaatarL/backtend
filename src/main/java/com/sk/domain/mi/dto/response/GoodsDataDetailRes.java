package com.sk.domain.mi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "miGoodsDataDetailRes")
public record GoodsDataDetailRes(

    @Schema(description = "상품 데이터 번호", example = "159")
    Integer goodsDataNo,

    @Schema(description = "상품 데이터 명", example = "structured name test - 001")
    String goodsDataNm

) {
}

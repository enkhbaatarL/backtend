package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record InsightsDataRes(
        @Schema(description = "프로바이더 번호", example = "")
        int providerNo,

        @Schema(description = "프로바이더 이름", example = "")
        String providerNm,

        @Schema(description = "상품 번호", example = "")
        int goodsNo,

        @Schema(description = "상품 이름", example = "")
        String goodsNm,

        @Schema(description = "상품데이터번호", example = "") 
        int goodsDataNo,

        @Schema(description = "상품데이터명", example = "") 
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "") 
        String goodsDataExpn,

        @Schema(description = "상품데이터 수정 시간", example = "") 
        LocalDateTime modifyDt) {

}
// @formatter:on
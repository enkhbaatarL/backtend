package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsUpdateReq(

        @Schema(description = "상품 이름", example = "제조사별 DRAM 생산량")
        String goodsNm,

        @Schema(description = "상품ID", example = "DRAM")
        String goodsId,

        @Schema(description = "상품 설명", example = "상품 설명")
        String goodsExpn,

        @Schema(description = "상품 사용유무", example = "true")
        Boolean isUsed) { 
        
        // @Schema(description = "회사공통사용가능여부", example = "true")
        // Boolean isPublic) {

}
// @formatter:on
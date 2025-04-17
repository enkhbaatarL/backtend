package com.sk.domain.mi.dto.response.insights;

import com.sk.domain.shared.entity.master.tmd.GoodsData;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

// @formatter:off
@Builder
public record GoodsDataDetailRes(
        @Schema(description = "상품데이터번호", example = "") 
        Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "") 
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "") 
        String goodsDataExpn,

        @Schema(description = "상품데이터요약내용", example = "") 
        String goodsDataSummaryCntt) {

            public static GoodsDataRes of(GoodsData goodsData) {
                return GoodsDataRes.builder()
                        .goodsDataNo(goodsData.getGoodsDataNo())
                        .goodsDataNm(goodsData.getGoodsDataNm())
                        .goodsDataExpn(goodsData.getGoodsDataExpn())
                        .goodsDataSummaryCntt(goodsData.getGoodsDataSummaryCntt())
                        .build();
            }
}
// @formatter:on
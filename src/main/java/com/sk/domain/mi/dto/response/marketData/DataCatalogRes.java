package com.sk.domain.mi.dto.response.marketData;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record DataCatalogRes(
        @Schema(description = "프로바이더 번호", example = "")
        int providerNo,

        @Schema(description = "상품 번호", example = "")
        int goodsNo,

        @Schema(description = "상품데이터번호", example = "") 
        int goodsDataNo,

        @Schema(description = "상품데이터명", example = "") 
        String goodsDataNm,

        @Schema(description = "상품데이터소스번호", example = "") 
        int goodsDataSrcNo,

        @Schema(description = "상품데이터소스명", example = "") 
        String goodsDataSrcNm,

        @Schema(description = "보기권한 여부", example = "true")
        boolean isViewable,
        
        @Schema(description = "수치", example = "95.5") 
        Double lastValue,

        @Schema(description = "상품데이터 수정 시간", example = "") 
        LocalDateTime modifyDt) {

            public DataCatalogRes(int providerNo, int goodsNo, int goodsDataNo, String goodsDataNm, int goodsDataSrcNo, String goodsDataSrcNm, 
            boolean isViewable, Double lastValue, LocalDateTime modifyDt) {
                this.providerNo = providerNo;
                this.goodsNo = goodsNo;
                this.goodsDataNo = goodsDataNo;
                this.goodsDataNm = goodsDataNm;
                this.goodsDataSrcNo = goodsDataSrcNo;
                this.goodsDataSrcNm = goodsDataSrcNm;
                this.isViewable = isViewable;
                this.lastValue = lastValue;
                this.modifyDt = modifyDt;
            }
}
// @formatter:on
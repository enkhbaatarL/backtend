package com.sk.domain.mi.dto.response.dashboard;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record LatestInsightsRes(
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

        @Schema(description = "상품데이터 수정 시간", example = "") 
        LocalDateTime modifyDt,

        @Schema(description = "보기 권한", example = "true")
        Boolean isViewable) {

        public LatestInsightsRes(int providerNo, String providerNm,
                                int goodsNo, String goodsNm,
                                int goodsDataNo, String goodsDataNm,
                                LocalDateTime modifyDt,
                                Boolean isViewable) {
            this.providerNo = providerNo;
            this.providerNm = providerNm;
            this.goodsNo = goodsNo;
            this.goodsNm = goodsNm;
            this.goodsDataNo = goodsDataNo;
            this.goodsDataNm = goodsDataNm;
            this.modifyDt = modifyDt;
            this.isViewable = isViewable != null ? isViewable : false; //
        }
}
// @formatter:on
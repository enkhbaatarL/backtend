package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record FocusedDataRes(
        @Schema(description = "상품데이터번호", example = "") 
        Integer goodsDataNo,

        @Schema(description = "자동으로 분류된 'Document Type' 라벨 노출", example = "Insight, Commodity Market Report 등") 
        String subCategoryNm,

        @Schema(description = "상품데이터명", example = "C&F Financial Corporation") 
        String goodsDataNm,

        @Schema(description = "마지막 업데이트일", example = "2025-02-12 15:00:00")
        LocalDateTime modifyDt,

        @Schema(description = "프로바이더명", example = "") 
        String providerNm) {

        public FocusedDataRes(Integer goodsDataNo, String subCategoryNm, String goodsDataNm, LocalDateTime modifyDt, String providerNm) {
            this.goodsDataNo = goodsDataNo;
            this.subCategoryNm = subCategoryNm;
            this.goodsDataNm = goodsDataNm;
            this.modifyDt = modifyDt;
            this.providerNm = providerNm;
        }

        public LocalDateTime getModifyDt() {
            return modifyDt;
        }
}
// @formatter:on
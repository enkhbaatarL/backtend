package com.sk.domain.mi.dto.response.insights;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.GoodsData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record FocusedDataRes(
        @Schema(description = "상품데이터번호", example = "") 
        Integer goodsDataNo,

        // 
        @Schema(description = "자동으로 분류된 'Document Type' 라벨 노출", example = "Insught, Commodity Market Report 등") 
        String subCategoryNm,

        @Schema(description = "상품데이터명", example = "") 
        String goodsDataNm,

        @Schema(description = "마지막 업데이트일", example = "2025-02-12 15:00:00")
        LocalDateTime modifyDt,

        @Schema(description = "프로바이더명", example = "") 
        String providerNm) {

        public LocalDateTime getModifyDt() {
            return modifyDt;
        }
}
// @formatter:on
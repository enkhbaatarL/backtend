package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDataCatgoryRes(
        @Schema(description = "코드명", example = "")
        String codeId,

        @Schema(description = "코드명", example = "")
        String codeNm,

        @Schema(description = "코드설명", example = "")
        String codeExpn) {
}
// @formatter:on
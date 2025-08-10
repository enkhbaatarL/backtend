package com.sk.domain.mi.dto.response.signalMap;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record SignalMapPeriodRes(
        @Schema(description = "코드ID", example = "") 
        String codeId,

        @Schema(description = "코드명", example = "") 
        String codeNm) {
}
// @formatter:on
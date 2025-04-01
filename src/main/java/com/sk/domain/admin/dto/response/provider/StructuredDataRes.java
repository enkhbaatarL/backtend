package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record StructuredDataRes(
        
        @Schema(description = "데이타타입", example = "") 
        String dataType,

        @Schema(description = "데이타명", example = "") 
        String dataNm,

        @Schema(description = "등록일시", example = "2025-02-12 15:00:00")
        LocalDateTime createDt) {

}
// @formatter:on
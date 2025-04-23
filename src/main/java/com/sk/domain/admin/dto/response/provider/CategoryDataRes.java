package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record CategoryDataRes(
        @Schema(description = "코드ID", example = "")
        String codeId,
        
        @Schema(description = "코드명", example = "")
        String codeNm) {
}
// @formatter:on
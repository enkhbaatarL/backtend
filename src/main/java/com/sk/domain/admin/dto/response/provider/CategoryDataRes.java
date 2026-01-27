package com.sk.domain.admin.dto.response.provider;

import com.sk.domain.admin.dto.CodeData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record CategoryDataRes(
        @Schema(description = "코드ID", example = "")
        String codeId,

        @Schema(description = "코드명", example = "")
        String codeNm
) implements CodeData{
}
// @formatter:on

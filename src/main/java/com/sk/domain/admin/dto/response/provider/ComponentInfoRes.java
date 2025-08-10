package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record ComponentInfoRes(
        @Schema(description = "컴포넌트유형ID", example = "")
        String componentTypeId,

        @Schema(description = "컴포넌트유형명", example = "")
        String componentTypeNm) {
}
// @formatter:on
package com.sk.domain.shared.dto.response.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 공통코드상세 응답 DTO
 */
public record CommonCodeDetailRes(
        @Schema(description = "코드ID", example = "CROIL")
        String codeId,

        @Schema(description = "코드명", example = "원유")
        String codeNm,

        @Schema(description = "코드설명", example = "원유")
        String codeExpn
) {
}

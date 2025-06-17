package com.sk.domain.mi.dto.response.signalMap;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record SignalMapFilterRes(
        @Schema(description = "코드ID", example = "")
        String codeId,

        @Schema(description = "코드명", example = "")
        String codeNm
) {
public SignalMapFilterRes(
        String codeId,
        String codeNm) {
    this.codeId = codeId;
    this.codeNm = codeNm;
  }
}
// @formatter:on

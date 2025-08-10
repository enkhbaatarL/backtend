package com.sk.domain.shared.dto.request.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// @formatter:off
public record FileDownloadReq(
    @NotNull
    @Schema(description = "파일번호", example = "1")
    Long fileNo,

    @NotBlank
    @Schema(description = "파일명", example = "sample.jpg")
    String fileNm,

    @NotNull
    @Schema(description = "파일크기", example = "1")
    Long fileSize
) {

}
// @formatter:on
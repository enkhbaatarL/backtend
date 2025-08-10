package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record UnstructuredDataRes(

        @Schema(description = "기준일자", example = "20250101")
        String criteriaDt,

        @Schema(description = "파일번호", example = "1")
        Long fileNo,

        @Schema(description = "파일명", example = "sample.jpg")
        String fileNm,

        @Schema(description = "파일크기(Byte)", example = "1024")
        Long fileSize) {

}
// @formatter:om
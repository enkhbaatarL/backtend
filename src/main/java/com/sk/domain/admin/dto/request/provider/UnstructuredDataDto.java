package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record UnstructuredDataDto(

        @Schema(description = "기준일자", example = "20250101")
        String criteriaDt,

        @Schema(description = "파일번호", example = "1") 
        Long fileNo) {

}
// @formatter:om
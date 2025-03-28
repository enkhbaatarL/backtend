package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// @formatter:off
public record UnstructuredDataReq(

        @NotBlank
        @Schema(description = "기준일자", example = "20250101")
        String criteriaDt,

        @NotBlank
        @Schema(description = "파일번호", example = "1") 
        Long fileNo) {

}
// @formatter:om
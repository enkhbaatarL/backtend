package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// @formatter:off
public record StructuredDataReq(
        
        @NotBlank
        @Schema(description = "컴포넌트유형ID", example = "") 
        String componentTypeId,

        @NotNull
        @Schema(description = "파일번호", example = "1") 
        Long fileNo) {

}
// @formatter:on
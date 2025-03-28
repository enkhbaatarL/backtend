package com.sk.domain.admin.service;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record UnStructuredDataRes(
        
        @Schema(description = "데이타타입", example = "") 
        String dataType,

        @Schema(description = "데이타명", example = "") 
        String dataNm,

        @Schema(description = "파일번호", example = "1") 
        Long fileNo) {

}
// @formatter:on

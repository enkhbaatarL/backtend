package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record StructuredDataDto(
        
        //데이타타입
        //데이타네임

        @Schema(description = "파일번호", example = "1") 
        Long fileNo) {

}
// @formatter:on
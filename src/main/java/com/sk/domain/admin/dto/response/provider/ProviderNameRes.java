package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record ProviderNameRes(
        @Schema(description = "프로바이더번호", example = "5000")
        Integer providerNo,

        @Schema(description = "프로바이더명", example = "레피니티브")
        String providerNm,
        
        @Schema(description = "사용여부", example = "true")
        Boolean isUsed) {

}
// @formatter:on
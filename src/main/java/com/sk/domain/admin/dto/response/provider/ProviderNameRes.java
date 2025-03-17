package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderNameRes(
    @Schema(description = "프로바이더번호", example = "5000") //
    Integer providerNo,

    @Schema(description = "프로바이더명", example = "레피니티브") //
    String providerNm) {

}
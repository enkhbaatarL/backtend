package com.sk.domain.shared.dto.request.rpa;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderDataColumnReq(
    @Schema(description = "프로바이더데이터컬럼명", example = "프로바이더데이터컬럼명")
    String providerDataColumnNm,

    @Schema(description = "데이터컬럼설명", example = "데이터컬럼설명")
    String dataColumnExpn
) {
}

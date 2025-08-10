package com.sk.domain.shared.dto.response.rpa;

import com.sk.domain.shared.entity.master.tmd.ProviderDataColumn;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderDataColumnRes(
    @Schema(description = "프로바이더데이터컬럼명", example = "프로바이더데이터컬럼명")
    String providerDataColumnNm,

    @Schema(description = "데이터컬럼설명", example = "데이터컬럼설명")
    String dataColumnExpn
) {
    public static ProviderDataColumnRes of(ProviderDataColumn providerDataColumn) {
        return new ProviderDataColumnRes(
            providerDataColumn.getId().getProviderDataColumnNm(),
            providerDataColumn.getDataColumnExpn()
        );
    }
}

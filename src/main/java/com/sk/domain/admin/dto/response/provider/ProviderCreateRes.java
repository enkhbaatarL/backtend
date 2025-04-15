package com.sk.domain.admin.dto.response.provider;

import com.sk.domain.shared.entity.master.tmd.Provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record ProviderCreateRes(
        @Schema(description = "프로바이더번호", example = "5000") 
        Integer providerNo) {

    public static ProviderCreateRes of(Provider provider) {
        return new ProviderCreateRes(provider.getProviderNo());
    }
}
// @formatter:on
package com.sk.domain.admin.dto.response.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CompanyProviderMapDetailRes(

    @JsonUnwrapped
    CompanyProviderMapRes companyProviderMapRes,

    @Schema(description = "프로바이더 기본 설명")
    String providerExpn

) {

    @Builder
    public CompanyProviderMapDetailRes(CompanyProviderMapRes companyProviderMapRes, String providerExpn) {
        this.companyProviderMapRes = companyProviderMapRes;
        this.providerExpn = providerExpn;
    }

    public static CompanyProviderMapDetailRes of(CustomerCompanyProviderMapping entity) {
        return CompanyProviderMapDetailRes.builder()
            .companyProviderMapRes(CompanyProviderMapRes.of(entity))
            .providerExpn(entity.getProvider().getProviderExpn())
            .build();
    }

}

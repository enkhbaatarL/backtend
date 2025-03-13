package com.sk.domain.admin.dto.response;

import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ProviderMappingRes(

    @Schema(description = "프로바이더번호", example = "")
    Integer providerNo,

    @Schema(description = "프로바이더명", example = "")
    String providerNm,

    @Schema(description = "고객회사번호", example = "")
    Integer companyNo,

    @Schema(description = "고객회사명", example = "")
    String companyNm

) {

    @Builder
    public ProviderMappingRes(Integer providerNo, String providerNm, Integer companyNo, String companyNm) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.companyNo = companyNo;
        this.companyNm = companyNm;
    }

    public static ProviderMappingRes of(CustomerCompanyProviderMapping providerMapping) {
        return ProviderMappingRes.builder()
            .providerNo(providerMapping.getProvider().getProviderNo())
            .providerNm(providerMapping.getProvider().getProviderNm())
            .companyNo(providerMapping.getCustomerCompany().getCustomerCompanyNo())
            .companyNm(providerMapping.getCustomerCompany().getCustomerCompanyNm())
            .build();
    }

}

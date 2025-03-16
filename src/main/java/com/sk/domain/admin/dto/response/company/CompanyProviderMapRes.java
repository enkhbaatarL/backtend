package com.sk.domain.admin.dto.response.company;

import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

public record CompanyProviderMapRes(

    @Schema(description = "프로바이더 번호", example = "159")
    Integer providerNo,

    @Schema(description = "프로바이더 명", example = "LSEG")
    String providerNm,

    @Schema(description = "고객회사 번호", example = "2005")
    Integer companyNo,

    @Schema(description = "고객회사 명", example = "sk이노베이션")
    String companyNm

) {

    @Builder
    public CompanyProviderMapRes(Integer providerNo, String providerNm, Integer companyNo, String companyNm) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.companyNo = companyNo;
        this.companyNm = companyNm;
    }

    public static CompanyProviderMapRes of(CustomerCompanyProviderMapping providerMapping) {
        return CompanyProviderMapRes.builder()
            .providerNo(providerMapping.getProvider().getProviderNo())
            .providerNm(providerMapping.getProvider().getProviderNm())
            .companyNo(providerMapping.getCustomerCompany().getCustomerCompanyNo())
            .companyNm(providerMapping.getCustomerCompany().getCustomerCompanyNm())
            .build();
    }

    public static List<CompanyProviderMapRes> ofList(List<CustomerCompanyProviderMapping> providerMappings) {
        return providerMappings.stream().map(CompanyProviderMapRes::of).collect(Collectors.toList());
    }

}

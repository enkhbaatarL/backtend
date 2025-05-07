package com.sk.domain.mi.dto.response;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiCompanyRes(

    @Schema(description = "고객회사번호")
    Integer customerCompanyNo,

    @Schema(description = "고객회사명")
    String customerCompanyNm,

    @Schema(description = "고객회사코드")
    String customerCompanyCd,

    @Schema(description = "고객회사MI플랫폼URL주소")
    String customerCompanyMipUrl
) {

    @Builder
    public MiCompanyRes(Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd, String customerCompanyMipUrl) {
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.customerCompanyCd = customerCompanyCd;
        this.customerCompanyMipUrl = customerCompanyMipUrl;
    }

    public static MiCompanyRes of(CustomerCompany customerCompany, String miDefaultDomain) {
        return MiCompanyRes.builder()
            .customerCompanyNo(customerCompany.getCustomerCompanyNo())
            .customerCompanyNm(customerCompany.getCustomerCompanyNm())
            .customerCompanyCd(customerCompany.getCustomerCompanyCd())
            .customerCompanyMipUrl(customerCompany.getCustomerCompanyMipUrl() != null ? customerCompany.getCustomerCompanyMipUrl() : miDefaultDomain)
            .build();
    }
}

package com.sk.domain.admin.dto.response.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CompanyProviderMapDetailRes(

    @JsonUnwrapped
    CompanyProviderMapRes companyProviderMapRes,

    @Schema(description = "프로바이더 기본 설명")
    String providerExpn,

    @JsonUnwrapped
    AuditInfo auditInfo

) {

    @Builder
    public CompanyProviderMapDetailRes(CompanyProviderMapRes companyProviderMapRes, String providerExpn, AuditInfo auditInfo) {
        this.companyProviderMapRes = companyProviderMapRes;
        this.providerExpn = providerExpn;
        this.auditInfo = auditInfo;
    }

    public static CompanyProviderMapDetailRes of(CustomerCompanyProviderMapping providerMapping) {
        return CompanyProviderMapDetailRes.builder()
            .companyProviderMapRes(CompanyProviderMapRes.of(providerMapping))
            .providerExpn(providerMapping.getProvider().getProviderExpn())
            .auditInfo(AuditInfo.of(
                providerMapping.getCreateUserId(),
                providerMapping.getCreateDt(),
                providerMapping.getModifyUserId(),
                providerMapping.getModifyDt(),
                providerMapping.accessCreatorNm(),
                providerMapping.accessCreatorDeptNm(),
                providerMapping.accessCreatorCompanyNm(),
                providerMapping.accessModifierNm(),
                providerMapping.accessModifierDeptNm(),
                providerMapping.accessModifierCompanyNm()
            ))
            .build();
    }

}

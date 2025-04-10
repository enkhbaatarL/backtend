package com.sk.domain.admin.dto.response.company;

import com.sk.domain.shared.dto.AuditInfo;
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
    String companyNm,

    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @Builder
    public CompanyProviderMapRes(Integer providerNo, String providerNm, Integer companyNo, String companyNm, AuditInfo auditInfo) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.companyNo = companyNo;
        this.companyNm = companyNm;
        this.auditInfo = auditInfo;
    }

    public static CompanyProviderMapRes of(CustomerCompanyProviderMapping entity) {
        return CompanyProviderMapRes.builder()
            .providerNo(entity.getProvider().getProviderNo())
            .providerNm(entity.getProvider().getProviderNm())
            .companyNo(entity.getCustomerCompany().getCustomerCompanyNo())
            .companyNm(entity.getCustomerCompany().getCustomerCompanyNm())
            .auditInfo(AuditInfo.ofDetail(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt(),
                entity.accessCreatorNm(),
                entity.accessCreatorDeptNm(),
                entity.accessCreatorCompanyNm(),
                entity.accessModifierNm(),
                entity.accessModifierDeptNm(),
                entity.accessModifierCompanyNm()
            ))
            .build();
    }

    public static CompanyProviderMapRes ofDefault(CustomerCompanyProviderMapping entity) {
        return CompanyProviderMapRes.builder()
            .providerNo(entity.getProvider().getProviderNo())
            .providerNm(entity.getProvider().getProviderNm())
            .companyNo(entity.getCustomerCompany().getCustomerCompanyNo())
            .companyNm(entity.getCustomerCompany().getCustomerCompanyNm())
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .build();
    }

}

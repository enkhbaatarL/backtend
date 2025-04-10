package com.sk.domain.admin.dto.response.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record CompanyRes(

    @Schema(description = "고객사 번호", example = "159")
    Integer companyNo,

    @Schema(description = "고객사 코드", example = "159")
    String companyCd,

    @Schema(description = "고객사 명", example = "SK이노베이션")
    String companyNm,

    @Schema(description = "사용시작 일자", example = "20250101")
    String usageBeginDate,

    @Schema(description = "사용종료 일자", example = "20251231")
    String usageEndDate,

    @Schema(description = "활성화 여부", example = "true")
    Boolean isActivated,

    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @Builder
    public CompanyRes(Integer companyNo, String companyCd, String companyNm, String usageBeginDate, String usageEndDate, Boolean isActivated, AuditInfo auditInfo) {
        this.companyNo = companyNo;
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.usageBeginDate = usageBeginDate;
        this.usageEndDate = usageEndDate;
        this.isActivated = isActivated;
        this.auditInfo = auditInfo;
    }

    @QueryProjection
    public CompanyRes(Integer companyNo, String companyCd, String companyNm, String usageBeginDate, String usageEndDate, Boolean isActivated, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt,
                      String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        this(companyNo, companyCd, companyNm, usageBeginDate, usageEndDate, isActivated,
            AuditInfo.ofDetail(createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm));
    }

    public static CompanyRes ofDefault(CustomerCompany entity) {
        return CompanyRes.builder()
            .companyNo(entity.getCustomerCompanyNo())
            .companyCd(entity.getCustomerCompanyCd())
            .companyNm(entity.getCustomerCompanyNm())
            .usageBeginDate(entity.getUseBeginDt())
            .usageEndDate(entity.getUseEndDt())
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .isActivated(entity.getIsUsed())
            .build();
    }

    public static CompanyRes of(CustomerCompany entity) {
        return CompanyRes.builder()
            .companyNo(entity.getCustomerCompanyNo())
            .companyCd(entity.getCustomerCompanyCd())
            .companyNm(entity.getCustomerCompanyNm())
            .usageBeginDate(entity.getUseBeginDt())
            .usageEndDate(entity.getUseEndDt())
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
            .isActivated(entity.getIsUsed())
            .build();
    }

}

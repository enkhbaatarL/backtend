package com.sk.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record CustomerCompanyRes(

    @Schema(description = "고객회사번호", example = "159")
    Integer companyNo,

    @Schema(description = "고객회사코드", example = "159")
    String companyCd,

    @Schema(description = "고객회사명", example = "SK이노베이션")
    String companyNm,

    @Schema(description = "사용시작일자", example = "20250101")
    String usageBeginDate,

    @Schema(description = "사용종료일자", example = "20251231")
    String usageEndDate,

    @JsonUnwrapped
    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @Builder
    public CustomerCompanyRes(Integer companyNo, String companyCd, String companyNm, String usageBeginDate, String usageEndDate, AuditInfo auditInfo) {
        this.companyNo = companyNo;
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.usageBeginDate = usageBeginDate;
        this.usageEndDate = usageEndDate;
        this.auditInfo = auditInfo;
    }

    @QueryProjection
    public CustomerCompanyRes(Integer companyNo, String companyCd, String companyNm, String usageBeginDate, String usageEndDate, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
        this(companyNo, companyCd, companyNm, usageBeginDate, usageEndDate,
            AuditInfo.of(createUserId, createDt, modifyUserId, modifyDt));
    }

    public static CustomerCompanyRes of(CustomerCompany savedEntity) {
        return CustomerCompanyRes.builder()
            .companyNo(savedEntity.getCustomerCompanyNo())
            .companyCd(savedEntity.getCustomerCompanyCd())
            .companyNm(savedEntity.getCustomerCompanyNm())
            .usageBeginDate(savedEntity.getUseBeginDt())
            .usageEndDate(savedEntity.getUseEndDt())
            .auditInfo(AuditInfo.of(
                savedEntity.getCreateUserId(),
                savedEntity.getCreateDt(),
                savedEntity.getModifyUserId(),
                savedEntity.getModifyDt()
            ))
            .build();
    }

}

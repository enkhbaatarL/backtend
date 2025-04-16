package com.sk.domain.admin.dto.response.company;

import java.util.List;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.CompanyType;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CompanyTypeAndCompanyRes(

        @Schema(description = "회사 타입", example = "CUSTOMER => 고객사 or MANAGER => 관리사")
        CompanyType companyType,

        @ArraySchema(
            schema = @Schema(implementation = Company.class),
            arraySchema = @Schema(description = "고객사 또는 관리사 목록")
        )
        List<Company> companies) {

    @Builder
    @QueryProjection
    public CompanyTypeAndCompanyRes(CompanyType companyType, List<Company> companies) {
        this.companyType = companyType;
        this.companies = companies;
    }
    
    public record Company(
        @Schema(description = "고객회사번호", example = "1")
        Integer companyNo,

        @Schema(description = "고객회사코드", example = "C000000001")
        String companyCd,

        @Schema(description = "고객회사명", example = "테스트 고객사")
        String companyNm
    ) {

        @Builder
        @QueryProjection
        public Company(Integer companyNo, String companyCd, String companyNm) {
            this.companyNo = companyNo;
            this.companyCd = companyCd;
            this.companyNm = companyNm;
        }
    }
}

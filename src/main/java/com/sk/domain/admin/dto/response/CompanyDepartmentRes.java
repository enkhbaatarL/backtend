package com.sk.domain.admin.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CompanyDepartmentRes(

        @Schema(description = "고객회사번호", example = "") Integer companyNo,

        @Schema(description = "고객회사코드", example = "") String companyCd,

        @Schema(description = "고객회사명", example = "") String companyNm,

        @Schema(description = "부서명", example = "") List<String> departmentNm) {

    @Builder
    public CompanyDepartmentRes(Integer companyNo, String companyCd, String companyNm, List<String> departmentNm) {
        this.companyNo = companyNo;
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.departmentNm = departmentNm;
    }
}

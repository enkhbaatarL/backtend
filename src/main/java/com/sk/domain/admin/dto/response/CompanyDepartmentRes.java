package com.sk.domain.admin.dto.response;

import java.util.List;

import com.querydsl.core.annotations.QueryProjection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CompanyDepartmentRes(

        @Schema(description = "고객회사번호", example = "1")
        Integer companyNo,

        @Schema(description = "고객회사코드", example = "C000000001")
        String companyCd,

        @Schema(description = "고객회사명", example = "테스트 고객사")
        String companyNm,

        @Schema(description = "부서명", example = "[\"개발팀\", \"기획팀\"]")
        List<String> departmentNm) {

    @Builder
    @QueryProjection
    public CompanyDepartmentRes(Integer companyNo, String companyCd, String companyNm, List<String> departmentNm) {
        this.companyNo = companyNo;
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.departmentNm = departmentNm;
    }
}

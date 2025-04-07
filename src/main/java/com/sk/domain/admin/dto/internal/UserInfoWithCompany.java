package com.sk.domain.admin.dto.internal;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

import lombok.Builder;

public record UserInfoWithCompany(
    
        // 사용자 이름
        String userNm,

        // 사용자 로그인 ID
        String userLoginId,

        // 직위명
        String positionNm,

        // 직급명
        String jobGradeNm,

        // 직책명
        String onDutyNm,

        // 부서명
        String departmentNm,

        // 고객회사번호
        CustomerCompany customerCompany,

        // 고객회사명
        String customerCompanyNm) {

    @Builder
    @QueryProjection
    public UserInfoWithCompany(String userNm, String userLoginId, String positionNm, String jobGradeNm, String onDutyNm,
            String departmentNm, CustomerCompany customerCompany, String customerCompanyNm) {
        this.userNm = userNm;
        this.userLoginId = userLoginId;
        this.positionNm = positionNm;
        this.jobGradeNm = jobGradeNm;
        this.onDutyNm = onDutyNm;
        this.departmentNm = departmentNm;
        this.customerCompany = customerCompany;
        this.customerCompanyNm = customerCompanyNm;
    }
}

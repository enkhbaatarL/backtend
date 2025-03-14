package com.sk.domain.admin.dto.dataResult;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

import lombok.Builder;

public record UserWithCompany(
    
        // 사용자 이름
        String userNm,

        // 사용자 로그인 ID
        String userLoginId,

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
    public UserWithCompany(String userNm, String userLoginId, String onDutyNm,
            String departmentNm, CustomerCompany customerCompany, String customerCompanyNm) {
        this.userNm = userNm;
        this.userLoginId = userLoginId;
        this.onDutyNm = onDutyNm;
        this.departmentNm = departmentNm;
        this.customerCompany = customerCompany;
        this.customerCompanyNm = customerCompanyNm;
    }
}

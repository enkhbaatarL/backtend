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
    String occupationDesignationNm,

    // 부서명
    String departmentNm,

    // 고객회사번호
    CustomerCompany customerCompany
) {

    @Builder
    @QueryProjection
    public UserWithCompany(String userNm, String userLoginId, String occupationDesignationNm,
            String departmentNm, CustomerCompany customerCompany) {
        this.userNm = userNm;
        this.userLoginId = userLoginId;
        this.occupationDesignationNm = occupationDesignationNm;
        this.departmentNm = departmentNm;
        this.customerCompany = customerCompany;
    }    
}

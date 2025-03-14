package com.sk.domain.admin.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tif.UserInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record UserInfoRes(

    @Schema(description = "IM코드", example = "") String imCd,

    @Schema(description = "사용자명", example = "") String userNm,

    @Schema(description = "사용자영문명", example = "") String userEnglishNm,

    @Schema(description = "로그인ID", example = "") String loginId,

    @Schema(description = "이메일주소", example = "") String emailAddress,

    @Schema(description = "회사코드", example = "") String companyCd,

    @Schema(description = "회사명", example = "") String companyNm,

    @Schema(description = "사원번호", example = "") String employeeNo,

    @Schema(description = "부서코드", example = "") String departmentCd,

    @Schema(description = "부서명", example = "") String departmentNm,

    @Schema(description = "직위코드", example = "") String positionCd,

    @Schema(description = "직위명", example = "") String positionNm,

    @Schema(description = "직급코드", example = "") String jobGradeCd,

    @Schema(description = "직급명", example = "") String jobGradeNm,

    @Schema(description = "직책코드", example = "") String onDutyCd,

    @Schema(description = "직책명", example = "") String onDutyNm

) {

    @Builder
    @QueryProjection
    public UserInfoRes(String imCd, String userNm, String userEnglishNm, String loginId, String emailAddress,
            String companyCd, String companyNm, String employeeNo, String departmentCd, String departmentNm,
            String positionCd, String positionNm, String jobGradeCd, String jobGradeNm, String onDutyCd,
            String onDutyNm) {
        this.imCd = imCd;
        this.userNm = userNm;
        this.userEnglishNm = userEnglishNm;
        this.loginId = loginId;
        this.emailAddress = emailAddress;
        this.companyCd = companyCd;
        this.companyNm = companyNm;
        this.employeeNo = employeeNo;
        this.departmentCd = departmentCd;
        this.departmentNm = departmentNm;
        this.positionCd = positionCd;
        this.positionNm = positionNm;
        this.jobGradeCd = jobGradeCd;
        this.jobGradeNm = jobGradeNm;
        this.onDutyCd = onDutyCd;
        this.onDutyNm = onDutyNm;
    }

    public static UserInfoRes of(UserInfo userInfo) {
        return UserInfoRes.builder()
                .imCd(userInfo.getImCd())
                .userNm(userInfo.getUserNm())
                .userEnglishNm(userInfo.getUserEnglishNm())
                .loginId(userInfo.getLoginId())
                .emailAddress(userInfo.getEmailAddress())
                .companyCd(userInfo.getCompanyCd())
                .companyNm(userInfo.getCompanyNm())
                .employeeNo(userInfo.getEmployeeNo())
                .departmentCd(userInfo.getDepartmentCd())
                .departmentNm(userInfo.getDepartmentNm())
                .positionCd(userInfo.getPositionCd())
                .positionNm(userInfo.getPositionNm())
                .jobGradeCd(userInfo.getJobGradeCd())
                .jobGradeNm(userInfo.getJobGradeNm())
                .onDutyCd(userInfo.getOnDutyCd())
                .onDutyNm(userInfo.getOnDutyNm())
                .build();
    }

}

package com.sk.domain.admin.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tif.UserInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record UserInfoRes(

        @Schema(description = "IM코드", example = "") String imCode,

        @Schema(description = "사용자명", example = "") String userName,

        @Schema(description = "사용자영문명", example = "") String userEnglishName,

        @Schema(description = "로그인ID", example = "") String loginId,

        @Schema(description = "이메일주소", example = "") String emailAddress,

        @Schema(description = "회사코드", example = "") String companyCode,

        @Schema(description = "회사명", example = "") String companyName,

        @Schema(description = "사원번호", example = "") String employeeNumber,

        @Schema(description = "부서코드", example = "") String departmentCode,

        @Schema(description = "부서명", example = "") String departmentName,

        @Schema(description = "직위코드", example = "") String positionCode,

        @Schema(description = "직위명", example = "") String positionName,

        @Schema(description = "직급코드", example = "") String jobGradeCode,

        @Schema(description = "직급명", example = "") String jobGradeName,

        @Schema(description = "직책코드", example = "") String onDutyCode,

        @Schema(description = "직책명", example = "") String onDutyName) {

    @Builder
    @QueryProjection
    public UserInfoRes(String imCode, String userName, String userEnglishName, String loginId, String emailAddress,
            String companyCode, String companyName, String employeeNumber, String departmentCode, String departmentName,
            String positionCode, String positionName, String jobGradeCode, String jobGradeName, String onDutyCode,
            String onDutyName) {
        this.imCode = imCode;
        this.userName = userName;
        this.userEnglishName = userEnglishName;
        this.loginId = loginId;
        this.emailAddress = emailAddress;
        this.companyCode = companyCode;
        this.companyName = companyName;
        this.employeeNumber = employeeNumber;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.positionCode = positionCode;
        this.positionName = positionName;
        this.jobGradeCode = jobGradeCode;
        this.jobGradeName = jobGradeName;
        this.onDutyCode = onDutyCode;
        this.onDutyName = onDutyName;
    }

    public static UserInfoRes of(UserInfo userInfo) {
        return UserInfoRes.builder()
                .imCode(userInfo.getImCode())
                .userName(userInfo.getUserName())
                .userEnglishName(userInfo.getUserEnglishName())
                .loginId(userInfo.getLoginId())
                .emailAddress(userInfo.getEmailAddress())
                .companyCode(userInfo.getCompanyCode())
                .companyName(userInfo.getCompanyName())
                .employeeNumber(userInfo.getEmployeeNumber())
                .departmentCode(userInfo.getDepartmentCode())
                .departmentName(userInfo.getDepartmentName())
                .positionCode(userInfo.getPositionCode())
                .positionName(userInfo.getPositionName())
                .jobGradeCode(userInfo.getJobGradeCode())
                .jobGradeName(userInfo.getJobGradeName())
                .onDutyCode(userInfo.getOnDutyCode())
                .onDutyName(userInfo.getOnDutyName())
                .build();
    }

}

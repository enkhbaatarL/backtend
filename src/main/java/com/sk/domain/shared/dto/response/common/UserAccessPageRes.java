package com.sk.domain.shared.dto.response.common;

import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserAccessPageType;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record UserAccessPageRes(

    @Schema(description = "사용자 번호", example = "10000")
    Integer userNo,

    @Schema(description = "사용자 명", example = "아이유")
    String userNm,

    @Schema(description = "사용자 로그인 아이디", example = "sk@sk.com")
    String userLoginId,

    @Schema(description = "사용자 역할(USER, ADMIN, OPERATOR)", example = "OPERATOR", allowableValues = {"USER", "ADMIN", "OPERATOR"})
    UserRoleCd userRoleCd,

    @Schema(description = "사용자 역할 명(사용자, 관리자, 운영자)", example = "OPERATOR", allowableValues = {"사용자", "관리자", "운영자"})
    String userRoleNm,

    @Schema(description = "직위명")
    String positionNm,

    @Schema(description = "직급명")
    String jobGradeNm,

    @Schema(description = "직책명")
    String onDutyNm,

    @Schema(description = "부서명")
    String departmentNm,

    @Schema(description = "타사서비스접근가능여부")
    Boolean isOtherServiceAccessPossible,

    @Schema(description = "개인정보동의여부")
    Boolean isIndividualInfoAgreement,

    @Schema(description = "고객회사 번호")
    Integer companyNo,

    @Schema(description = "고객회사 명")
    String companyNm,

    @Schema(description = "페이지 접근 퍼미션 타입", example = "MI or ADMIN or BOTH")
    UserAccessPageType userAccessPageType
) {

    @Builder
    public UserAccessPageRes(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd, String userRoleNm, String positionNm, String jobGradeNm, String onDutyNm, String departmentNm, Boolean isOtherServiceAccessPossible, Boolean isIndividualInfoAgreement, Integer companyNo, String companyNm, UserAccessPageType userAccessPageType) {
        this.userNo = userNo;
        this.userNm = userNm;
        this.userLoginId = userLoginId;
        this.userRoleCd = userRoleCd;
        this.userRoleNm = userRoleNm;
        this.positionNm = positionNm;
        this.jobGradeNm = jobGradeNm;
        this.onDutyNm = onDutyNm;
        this.departmentNm = departmentNm;
        this.isOtherServiceAccessPossible = isOtherServiceAccessPossible;
        this.isIndividualInfoAgreement = isIndividualInfoAgreement;
        this.companyNo = companyNo;
        this.companyNm = companyNm;
        this.userAccessPageType = userAccessPageType;
    }

    public static UserAccessPageRes of(User entity, UserAccessPageType userAccessPageType) {
        return UserAccessPageRes.builder()
            .userNo(entity.getUserNo())
            .userNm(entity.getUserNm())
            .userLoginId(entity.getUserLoginId())
            .userRoleCd(entity.getUserRoleCd())
            .userRoleNm(entity.getUserRoleCd().getDescription())
            .positionNm(entity.getPositionNm())
            .jobGradeNm(entity.getJobGradeNm())
            .onDutyNm(entity.getOnDutyNm())
            .departmentNm(entity.getDepartmentNm())
            .isOtherServiceAccessPossible(entity.getIsOtherServiceAccessPossible())
            .isIndividualInfoAgreement(entity.getIsIndividualInfoAgreement())
            .companyNo(entity.getCustomerCompany().getCustomerCompanyNo())
            .companyNm(entity.getCustomerCompany().getCustomerCompanyNm())
            .userAccessPageType(userAccessPageType)
            .build();
    }

}

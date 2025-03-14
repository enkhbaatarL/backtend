package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record UserWithCompanyRes(

	@Schema(description = "사용자번호", example = "") Integer userNo,

	@Schema(description = "사용자명", example = "") String userNm,

	@Schema(description = "사용자로그인ID", example = "") String userLoginId,

	@Schema(description = "사용자역할코드", example = "") UserRoleCd userRoleCd,

	@Schema(description = "직책명", example = "") String onDutyNm,

	@Schema(description = "부서명", example = "") String departmentNm,

	@Schema(description = "타사서비스접근가능여부", example = "") Boolean isOtherServiceAccessPossible,

	@Schema(description = "개인정보동의여부", example = "") Boolean isIndividualInfoAgreement,

	@Schema(description = "개인정보동의일시", example = "") LocalDateTime individualInfoAgreementDtm,

	@Schema(description = "고객사번호", example = "") CustomerCompany customerCompanyNo,

	@Schema(description = "고객사명", example = "") String customerCompanyNm,

	@JsonUnwrapped @Schema(description = "감사 정보") AuditInfo auditInfo

) {

	@Builder
	public UserWithCompanyRes(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd,
			String onDutyNm, String departmentNm, Boolean isOtherServiceAccessPossible,
			Boolean isIndividualInfoAgreement,
			LocalDateTime individualInfoAgreementDtm, CustomerCompany customerCompanyNo, String customerCompanyNm,
			AuditInfo auditInfo) {
		this.userNo = userNo;
		this.userNm = userNm;
		this.userLoginId = userLoginId;
		this.userRoleCd = userRoleCd;
		this.onDutyNm = onDutyNm;
		this.departmentNm = departmentNm;
		this.isOtherServiceAccessPossible = isOtherServiceAccessPossible;
		this.isIndividualInfoAgreement = isIndividualInfoAgreement;
		this.individualInfoAgreementDtm = individualInfoAgreementDtm;
		this.customerCompanyNo = customerCompanyNo;
		this.customerCompanyNm = customerCompanyNm;
		this.auditInfo = auditInfo;
	}

	public static UserWithCompanyRes of(User user) {
		return UserWithCompanyRes.builder()
				.userNo(user.getUserNo())
				.userNm(user.getUserNm())
				.userLoginId(user.getUserLoginId())
				.userRoleCd(user.getUserRoleCd())
				.onDutyNm(user.getOnDutyNm())
				.departmentNm(user.getDepartmentNm())
				.isOtherServiceAccessPossible(user.getIsOtherServiceAccessPossible())
				.isIndividualInfoAgreement(user.getIsIndividualInfoAgreement())
				.individualInfoAgreementDtm(user.getIndividualInfoAgreementDtm())
				.customerCompanyNo(user.getCustomerCompany())
				.customerCompanyNm(user.getCustomerCompany().getCustomerCompanyNm())
				.auditInfo(AuditInfo.of(user.getCreateUserId(), user.getCreateDt(), user.getModifyUserId(),
						user.getModifyDt()))
				.build();
	}
}

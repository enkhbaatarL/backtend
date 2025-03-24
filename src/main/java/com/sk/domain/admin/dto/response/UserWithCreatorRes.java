package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record UserWithCreatorRes(
		@Schema(description = "사용자번호", example = "")
		Integer userNo,

		@Schema(description = "사용자명", example = "")
		String userNm,

		@Schema(description = "사용자로그인ID", example = "")
		String userLoginId,

		@Schema(description = "사용자역할코드", example = "")
		UserRoleCd userRoleCd,

		@Schema(description = "직책명", example = "")
		String onDutyNm,

		@Schema(description = "부서명", example = "")
		String departmentNm,

		@Schema(description = "타사서비스접근가능여부", example = "")
		Boolean isOtherServiceAccessPossible,

		@Schema(description = "개인정보동의여부", example = "")
		Boolean isIndividualInfoAgreement,

		@Schema(description = "개인정보동의일시", example = "")
		LocalDateTime individualInfoAgreementDtm,

		@Schema(description = "고객회사번호", example = "")
		Integer customerCompanyNo,

		@Schema(description = "고객회사명", example = "")
		String customerCompanyNm,

		@Schema(description = "등록자 이름", example = "")
		String creatorUserNm,

		@Schema(description = "등록자 부서명", example = "")
		String creatorDepartmentNm,

		@Schema(description = "등록자 직책명", example = "")
		String creatorOnDutyNm,

		@Schema(description = "등록자 회사번호", example = "")
		Integer creatorCompanyNo,

		@Schema(description = "등록자 회사명", example = "")
		String creatorCompanyNm,

		@JsonUnwrapped
		@Schema(description = "감사 정보")
		AuditInfo auditInfo

) {

	@Builder
	public UserWithCreatorRes(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd, String onDutyNm,
			String departmentNm, Boolean isOtherServiceAccessPossible, Boolean isIndividualInfoAgreement,
			LocalDateTime individualInfoAgreementDtm, Integer customerCompanyNo, String customerCompanyNm,
			String creatorUserNm, String creatorDepartmentNm, String creatorOnDutyNm, Integer creatorCompanyNo,
			String creatorCompanyNm, AuditInfo auditInfo) {
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
		this.creatorUserNm = creatorUserNm;
		this.creatorDepartmentNm = creatorDepartmentNm;
		this.creatorOnDutyNm = creatorOnDutyNm;
		this.creatorCompanyNo = creatorCompanyNo;
		this.creatorCompanyNm = creatorCompanyNm;
		this.auditInfo = auditInfo;
	}

	@QueryProjection
	public UserWithCreatorRes(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd,
			String onDutyNm, String departmentNm, Boolean isOtherServiceAccessPossible,
			Boolean isIndividualInfoAgreement, LocalDateTime individualInfoAgreementDtm,
			Integer customerCompanyNo, String customerCompanyNm,
			String creatorUserNm, String creatorDepartmentNm, String creatorOnDutyNm,
			Integer creatorCompanyNo, String creatorCompanyNm,
			String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
		this(userNo, userNm, userLoginId, userRoleCd, onDutyNm, departmentNm,
				isOtherServiceAccessPossible, isIndividualInfoAgreement, individualInfoAgreementDtm,
				customerCompanyNo, customerCompanyNm, creatorUserNm, creatorDepartmentNm,
				creatorOnDutyNm, creatorCompanyNo, creatorCompanyNm,
				AuditInfo.of(createUserId, createDt, modifyUserId, modifyDt));
	}
}

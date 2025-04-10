package com.sk.domain.admin.dto.response.user;

import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record UserDetailRes(

		@Schema(description = "사용자번호", example = "1")
		Integer userNo,

		@Schema(description = "사용자명", example = "홍길동")
		String userNm,

		@Schema(description = "사용자로그인ID", example = "hong@sk.com")
		String userLoginId,

		@Schema(description = "사용자역할코드", example = "ADMIN")
		UserRoleCd userRoleCd,

		@Schema(description = "직위명", example = "사장")
		String positionNm,

		@Schema(description = "직급명", example = "과장")
		String jobGradeNm,

		@Schema(description = "직책명", example = "사장")
		String onDutyNm,

		@Schema(description = "부서명", example = "총무팀")
		String departmentNm,

		@Schema(description = "타사서비스접근가능여부", example = "true")
		Boolean isOtherServiceAccessPossible,

		@Schema(description = "개인정보동의여부", example = "true")
		Boolean isIndividualInfoAgreement,

		@Schema(description = "개인정보동의일시", example = "2025-01-01 12:00:00")
		LocalDateTime individualInfoAgreementDtm,

		@Schema(description = "고객사번호", example = "1")
		Integer customerCompanyNo,

		@Schema(description = "고객사명", example = "SK하이닉스")
		String customerCompanyNm,

		@Schema(description = "감사 정보")
		AuditInfo auditInfo

) {
	@Builder
	public UserDetailRes(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd,
			String positionNm, String jobGradeNm, String onDutyNm,
			String departmentNm, Boolean isOtherServiceAccessPossible, Boolean isIndividualInfoAgreement,
			LocalDateTime individualInfoAgreementDtm, Integer customerCompanyNo, String customerCompanyNm,
			AuditInfo auditInfo) {
		this.userNo = userNo;
		this.userNm = userNm;
		this.userLoginId = userLoginId;
		this.userRoleCd = userRoleCd;
		this.positionNm = positionNm;
		this.jobGradeNm = jobGradeNm;
		this.onDutyNm = onDutyNm;
		this.departmentNm = departmentNm;
		this.isOtherServiceAccessPossible = isOtherServiceAccessPossible;
		this.isIndividualInfoAgreement = isIndividualInfoAgreement;
		this.individualInfoAgreementDtm = individualInfoAgreementDtm;
		this.customerCompanyNo = customerCompanyNo;
		this.customerCompanyNm = customerCompanyNm;
		this.auditInfo = auditInfo;
	}

	@QueryProjection
	public UserDetailRes(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd,
			String positionNm, String jobGradeNm, String onDutyNm, String departmentNm,
			Boolean isOtherServiceAccessPossible, Boolean isIndividualInfoAgreement,
			LocalDateTime individualInfoAgreementDtm, Integer customerCompanyNo, String customerCompanyNm,
			String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt,
			String createUserNm, String createDepartmentNm, String createCompanyNm,
			String modifyUserNm, String modifyDepartmentNm, String modifyCompanyNm) {
		this(userNo, userNm, userLoginId, userRoleCd, positionNm, jobGradeNm, onDutyNm, departmentNm,
				isOtherServiceAccessPossible, isIndividualInfoAgreement, individualInfoAgreementDtm,
				customerCompanyNo, customerCompanyNm,
				AuditInfo.ofDetail(
						createUserId, createDt, modifyUserId, modifyDt,
						createUserNm, createDepartmentNm, createCompanyNm,
						modifyUserNm, modifyDepartmentNm, modifyCompanyNm)

		);
	}

	public static UserDetailRes of(User user) {
		return UserDetailRes.builder()
				.userNo(user.getUserNo())
				.userNm(user.getUserNm())
				.userLoginId(user.getUserLoginId())
				.userRoleCd(user.getUserRoleCd())
				.positionNm(user.getPositionNm())
				.jobGradeNm(user.getJobGradeNm())
				.onDutyNm(user.getOnDutyNm())
				.departmentNm(user.getDepartmentNm())
				.isOtherServiceAccessPossible(user.getIsOtherServiceAccessPossible())
				.isIndividualInfoAgreement(user.getIsIndividualInfoAgreement())
				.individualInfoAgreementDtm(user.getIndividualInfoAgreementDtm())
				.customerCompanyNo(user.getCustomerCompany().getCustomerCompanyNo())
				.customerCompanyNm(user.getCustomerCompany().getCustomerCompanyNm())
				.auditInfo(AuditInfo.ofDetail(
						user.getCreateUserId(), user.getCreateDt(), user.getModifyUserId(),
						user.getModifyDt(), user.accessCreatorNm(), user.accessCreatorDeptNm(),
						user.accessCreatorCompanyNm(), user.accessModifierNm(), user.accessModifierDeptNm(),
						user.accessModifierCompanyNm()))
				.build();
	}
}

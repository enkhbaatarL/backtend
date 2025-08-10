package com.sk.domain.admin.dto.response.user;

import java.time.LocalDateTime;

import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record UserRes(

		@Schema(description = "사용자번호", example = "1") Integer userNo,

		@Schema(description = "사용자명", example = "홍길동") String userNm,

		@Schema(description = "사용자로그인ID", example = "hong@sk.com") String userLoginId,

		@Schema(description = "사용자역할코드", example = "ADMIN") UserRoleCd userRoleCd,

		@Schema(description = "직위명", example = "사장") String positionNm,

		@Schema(description = "직급명", example = "과장") String jobGradeNm,

		@Schema(description = "직책명", example = "사장") String onDutyNm,

		@Schema(description = "부서명", example = "총무팀") String departmentNm,

		@Schema(description = "타사서비스접근가능여부", example = "true") Boolean isOtherServiceAccessPossible,

		@Schema(description = "개인정보동의여부", example = "true") Boolean isIndividualInfoAgreement,

		@Schema(description = "개인정보동의일시", example = "2025-01-01 12:00:00") LocalDateTime individualInfoAgreementDtm,

		@Schema(description = "고객사번호", example = "1") Integer customerCompanyNo,

		@Schema(description = "사용자 알림 설정 여부", example = "true") Boolean isUserNotificationSetting,

		@Schema(description = "감사 정보") AuditInfo auditInfo

) {
	@Builder
	public UserRes(Integer userNo, String userNm, String userLoginId, UserRoleCd userRoleCd,
			String positionNm, String jobGradeNm, String onDutyNm,
			String departmentNm, Boolean isOtherServiceAccessPossible, Boolean isIndividualInfoAgreement,
			LocalDateTime individualInfoAgreementDtm, Integer customerCompanyNo, Boolean isUserNotificationSetting,
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
		this.isUserNotificationSetting = isUserNotificationSetting;
		this.auditInfo = auditInfo;
	}

	public static UserRes ofSimple(User user) {
		return UserRes.builder()
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
				.isUserNotificationSetting(user.getIsUserNotificationSetting())
				.auditInfo(AuditInfo.ofSimple(user.getCreateUserId(), user.getCreateDt(), user.getModifyUserId(),
						user.getModifyDt()))
				.build();
	}
}

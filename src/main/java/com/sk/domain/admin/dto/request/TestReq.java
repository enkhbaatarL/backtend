package com.sk.domain.admin.dto.request;

import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TestReq() {
	public record getUserInfo(

			@Email @NotEmpty @Schema(description = "이메일 주소", example = "mk.jang@twolinecode.com") String email,

			@NotEmpty @Schema(description = "사용자 명", example = "장민규") String name) {
	}

	public record userLogin(
			@NotEmpty @Schema(description = "사용자 아이디", example = "mk.jang@twolinecode.com") String loginUserId
	) {
	}

	public record adminLogin(
			@NotEmpty @Schema(description = "사용자 아이디", example = "mk.jang@twolinecode.com") String loginUserId,
			@NotNull @Schema(description = "사용자 역할", example = "ADMIN") UserRoleCd userRoleCd
	) {
	}

	public record verifyToken(
			@NotEmpty @Schema(description = "토큰", example = "1234567890") String token
	) {
	}

	public record uploadStructuredDataFile(
			@NotNull @Schema(description = "컴포넌트 타입 ID", example = "TYPE_ID") String componentTypeId,
			@Schema(description = "기준일자", example = "20250424") String criteriaDt
	) {
	}

	public record saveComponent(
			@NotEmpty @Schema(description = "컴포넌트 타입 ID", example = "ABSD") String componentTypeId,
			@NotEmpty @Schema(description = "컴포넌트 타입 명", example = "정형 데이터") String componentTypeNm,
			@NotEmpty @Schema(description = "상품 데이터 소스 명", example = "SKCC") String goodsDataSourceNm,
			@NotEmpty @Schema(description = "대상 데이터베이스 명", example = "SKCC") String targetDbNm,
			@NotEmpty @Schema(description = "대상 테이블 명", example = "tif_ctfcftotncotr_imsi") String targetTableNm,
			@Schema(description = "쿼리 조건 내용", example = "1 = 1") String queryConditionCntt,
			@NotEmpty @Schema(description = "테이블 컬럼 목록 명", example = "data_reg_sn,trdprc_1,mid_price,settle,vol_dec,fut_basis") String tableColumnListNm,
			@NotEmpty @Schema(description = "엑셀 컬럼 목록 명", example = "trdprc_1,mid_price,settle,vol_dec,fut_basis") String excelColumnListNm,
			@Schema(description = "엑셀 조건 내용", example = "1") String excelConditionCntt
	) {
	}

	public record sendEmail(
			@NotEmpty @Schema(description = "이메일 주소", example = "abc@twolinecode.com") String email
	) {
	}

	public record deleteRedisCache(
			@Schema(description = "삭제하고자 하는 redis 키(type : key*), 키가 없으면 모든 키를 삭제합니다.", example = "abc") String key
	) {
	}
}

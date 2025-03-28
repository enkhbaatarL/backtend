package com.sk.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record TestReq() {
	public record getUserInfo(

			@Email @NotEmpty @Schema(description = "이메일 주소", example = "mk.jang@twolinecode.com") String email,

			@NotEmpty @Schema(description = "사용자 명", example = "장민규") String name) {
	}

	public record userLogin(
			@NotEmpty @Schema(description = "사용자 아이디", example = "mk.jang@twolinecode.com") String loginUserId
	) {
	}

	public record verifyToken(
			@NotEmpty @Schema(description = "토큰", example = "1234567890") String token
	) {
	}
}

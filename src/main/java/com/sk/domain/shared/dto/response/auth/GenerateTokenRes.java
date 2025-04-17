package com.sk.domain.shared.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record GenerateTokenRes(
        @Schema(description = "액세스 토큰", example = "asdasxcqw123123123") String accessToken
) {
    public static GenerateTokenRes of(String accessToken) {
        return new GenerateTokenRes(accessToken);
    }
}

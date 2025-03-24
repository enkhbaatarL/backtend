package com.sk.domain.shared.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record GenerateTokenRes(

        @Schema(description = "액세스 토큰", example = "asdasxcqw123123123") String accessToken,

        @Schema(description = "리프레시 토큰", example = "asdasxcqw123123123") String refreshToken

) {

        public static GenerateTokenRes of(String accessToken, String refreshToken) {
                return new GenerateTokenRes(accessToken, refreshToken);
        }

}

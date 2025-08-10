package com.sk.domain.shared.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record OAuth2Res(

    @Schema(description = "엑세스 토큰", example = "")
    String accessToken,

    @Schema(description = "토큰 타입", example = "Bearer")
    String tokenType,

    @Schema(description = "엑세스 만료시간(초)", example = "")
    long expiresInSeconds,

    @Schema(description = "토큰 발행일시", example = "")
    String issuedAt

) {

    @Builder
    public OAuth2Res(String accessToken, String tokenType, long expiresInSeconds, String issuedAt) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.issuedAt = issuedAt;
    }

    public static OAuth2Res of(String tokenValue, String tokenType, long expiresIn, String issuedAtFormatted) {
        return OAuth2Res.builder()
            .accessToken(tokenValue)
            .tokenType(tokenType)
            .expiresInSeconds(expiresIn)
            .issuedAt(issuedAtFormatted)
            .build();
    }

}

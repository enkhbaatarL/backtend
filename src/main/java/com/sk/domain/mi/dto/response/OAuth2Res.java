package com.sk.domain.mi.dto.response;

import lombok.Builder;

public record OAuth2Res(

    String accessToken,
    String tokenType,
    long expiresInSeconds,
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

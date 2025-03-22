package com.sk.domain.admin.dto.response;

import lombok.Builder;

public record TestLoginRes(

        String accessToken,
        String refreshToken

) {

    @Builder
    public TestLoginRes(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static TestLoginRes of(String accessToken, String refreshToken) {
        return TestLoginRes.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

}

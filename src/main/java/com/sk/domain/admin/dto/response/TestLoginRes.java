package com.sk.domain.admin.dto.response;

import lombok.Builder;

public record TestLoginRes(

        String accessToken

) {

    @Builder
    public TestLoginRes(String accessToken) {
        this.accessToken = accessToken;
    }

    public static TestLoginRes of(String accessToken) {
        return TestLoginRes.builder()
                .accessToken(accessToken)
                .build();
    }

}

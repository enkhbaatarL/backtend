package com.sk.domain.mi.dto.request;

public record OauthReq(
    String grantType,
    String clientId,
    String clientSecret
) {
}

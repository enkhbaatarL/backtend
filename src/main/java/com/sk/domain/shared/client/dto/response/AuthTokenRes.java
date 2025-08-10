package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// @formatter:off
public record AuthTokenRes(
        @JsonProperty("access_token") 
        String accessToken) {

}
// @formatter:on
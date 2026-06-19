package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FileCryptoStatusRes(
        @JsonProperty("Result")
        @JsonAlias("result")
        Boolean result,
        @JsonProperty("ErrorMsg")
        @JsonAlias("errorMsg")
        String errorMsg) {
}

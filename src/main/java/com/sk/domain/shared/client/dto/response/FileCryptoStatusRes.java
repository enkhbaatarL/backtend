package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FileCryptoStatusRes(
        Boolean result,
        @JsonProperty("ErrorMsag")
        @JsonAlias({"ErrorMsg", "errorMsg"})
        String errorMsg) {
}

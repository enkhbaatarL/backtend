package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record FileCryptoUploadRes(
        @JsonProperty("Result")
        @JsonAlias("result")
        int result,
        @JsonProperty("ErrorMsg")
        @JsonAlias("errorMsg")
        String errorMsg,
        @JsonProperty("FID")
        @JsonAlias("fid")
        UUID fid) {
}

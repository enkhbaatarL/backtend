package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record FileCryptoDecryptRes(
        @JsonProperty("OutputFilePath")
        @JsonAlias("outputFilePath")
        String outputFilePath,
        @JsonProperty("DataSize")
        @JsonAlias("dataSize")
        Long dataSize,
        @JsonProperty("OperationUserID")
        @JsonAlias("operationUserId")
        String operationUserId,
        @JsonProperty("Status")
        @JsonAlias("status")
        Integer status,
        @JsonProperty("ErrorMsg")
        @JsonAlias("errorMsg")
        String errorMsg,
        @JsonProperty("FID")
        @JsonAlias("fid")
        UUID fid) {
}

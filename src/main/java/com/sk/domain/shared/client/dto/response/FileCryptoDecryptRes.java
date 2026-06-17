package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record FileCryptoDecryptRes(
        @JsonProperty("OutputFilePath")
        String outputFilePath,
        @JsonProperty("DataSize")
        Long dataSize,
        @JsonProperty("OperationUserId")
        String operationUserId,
        @JsonProperty("Status")
        Integer status,
        @JsonProperty("ErrorMsg")
        String errorMsg,
        @JsonProperty("FID")
        UUID fid) {
}

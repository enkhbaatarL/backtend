package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record FileCryptoEncryptRes(
        @JsonProperty("OutputFilePath")
        String outputFilePath,
        @JsonProperty("DataSize")
        Long dataSize,
        @JsonProperty("Status")
        Integer status,
        @JsonProperty("ErrorMsg")
        String errorMsg,
        @JsonProperty("OperationUserId")
        String operationUserId,
        @JsonProperty("FID")
        UUID fid,
        @JsonProperty("LabelID")
        String labelId) {
}

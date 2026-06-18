package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record FileCryptoEncryptRes(
        @JsonProperty("OutputFilePath")
        @JsonAlias("outputFilePath")
        String outputFilePath,
        @JsonProperty("DataSize")
        @JsonAlias("dataSize")
        Long dataSize,
        @JsonProperty("Status")
        @JsonAlias("status")
        Integer status,
        @JsonProperty("ErrorMsg")
        @JsonAlias("errorMsg")
        String errorMsg,
        @JsonProperty("OperationUserID")
        @JsonAlias("operationUserId")
        String operationUserId,
        @JsonProperty("FID")
        @JsonAlias("fid")
        UUID fid,
        @JsonProperty("LabelID")
        @JsonAlias("labelId")
        String labelId) {
}

package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// @formatter:off
public record AXMetaLookUpRes(
       @JsonProperty("file_id")
            String fileId
    ){
}
// @formatter:on
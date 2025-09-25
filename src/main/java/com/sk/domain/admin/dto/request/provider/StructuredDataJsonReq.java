package com.sk.domain.admin.dto.request.provider;

import com.fasterxml.jackson.databind.JsonNode;

public record StructuredDataJsonReq (
    StructuredDataFileReq metadata,
    JsonNode data
){

}

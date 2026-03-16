package com.sk.domain.mi.dto.request.newsletter;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonPatchOpReq(
    String op,
    String path,
    JsonNode value
) {
}

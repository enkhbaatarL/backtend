package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonPatchOpRes(
    String op,
    String path,
    JsonNode value
) {
}

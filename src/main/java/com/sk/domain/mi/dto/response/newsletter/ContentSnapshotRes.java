package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;

public record ContentSnapshotRes(
    Integer revisionNo,
    JsonNode data
) {
}

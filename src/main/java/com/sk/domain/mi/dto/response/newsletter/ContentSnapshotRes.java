package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record ContentSnapshotRes(
    Integer revisionNo,
    JsonNode data,
    LocalDateTime savedAt
) {
}

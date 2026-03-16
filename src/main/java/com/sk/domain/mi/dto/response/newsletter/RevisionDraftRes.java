package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record RevisionDraftRes(
    Integer revisionNo,
    JsonNode content,
    String revisionType,
    LocalDateTime updatedAt,
    String modifier
) {
}

package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;

public record NewsletterDetailV2Res(
    Integer newsletterId,
    Integer issueNo,
    Integer schemaVersion,
    String source,
    Boolean subscribed,
    JsonNode snapshot
) {
}

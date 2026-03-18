package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;

public record NewsletterIssueOriginalSnapshotRes(
    Integer newsletterId,
    Integer issueNo,
    Integer schemaVersion,
    String source,
    JsonNode snapshot
) {
}

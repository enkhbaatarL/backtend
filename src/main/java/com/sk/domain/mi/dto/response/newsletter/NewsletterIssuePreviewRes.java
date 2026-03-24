package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;

public record NewsletterIssuePreviewRes(
    Integer newsletterId,
    Integer issueNo,
    JsonNode snapshot
) {
}

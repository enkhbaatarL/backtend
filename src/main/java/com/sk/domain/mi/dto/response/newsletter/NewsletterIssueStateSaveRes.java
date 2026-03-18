package com.sk.domain.mi.dto.response.newsletter;

public record NewsletterIssueStateSaveRes(
    Integer newsletterId,
    Integer issueNo,
    Long issueOriginalId,
    Integer savedSectionCount,
    Integer savedBlockCount,
    NewsletterIssuePreviewRes preview
) {
}

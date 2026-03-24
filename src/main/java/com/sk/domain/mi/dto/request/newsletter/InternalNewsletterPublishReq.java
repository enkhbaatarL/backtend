package com.sk.domain.mi.dto.request.newsletter;

public record InternalNewsletterPublishReq(
    Integer newsletterMasterId,
    Integer issueNo,
    Integer publishHistoryId
) {
}

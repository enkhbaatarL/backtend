package com.sk.domain.mi.dto.response.newsletter;

public record NewsletterOriginalSnapshotReplayRes(
    Integer newsletterId,
    Integer issueNo,
    Long issueOriginalId,
    Integer deletedRevisionCount,
    Integer deletedDeltaCount,
    Integer deletedSnapshotCount,
    String message
) {
}

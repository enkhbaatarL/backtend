package com.sk.domain.mi.dto.response.newsletter;

import java.time.LocalDateTime;

public record RevisionSaveRes(
    Long revisionId,
    Integer revisionNo,
    String editStatus,
    LocalDateTime savedAt,
    LocalDateTime lastAutoSavedAt,
    Boolean snapshotSaved
) {
}

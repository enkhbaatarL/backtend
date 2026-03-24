package com.sk.domain.mi.dto.response.newsletter;

import java.time.LocalDateTime;

public record RevisionItemRes(
    Long seq,
    String revisionType,
    LocalDateTime updatedAt,
    String modifier
) {
}

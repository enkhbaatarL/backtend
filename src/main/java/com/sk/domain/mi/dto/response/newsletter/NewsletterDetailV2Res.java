package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.lang.Nullable;

public record NewsletterDetailV2Res(
    Integer newsletterId,
    Integer issueNo,
    Integer schemaVersion,
    LocalDateTime publishedAt,
    Boolean subscribed,

    @Nullable
    RevisionStateRes revision,

    ContentSnapshotRes latestSnapshot,
    Integer deltaCount,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<JsonPatchOpRes> deltaChanges
) {
    public record RevisionStateRes(
        Long revisionId,
        String editStatus,
        Integer currentRevisionNo,
        Boolean hasDraft,
        @Nullable Integer draftRevisionNo
    ) {}
}

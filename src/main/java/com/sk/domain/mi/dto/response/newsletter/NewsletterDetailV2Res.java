package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.lang.Nullable;

public record NewsletterDetailV2Res(
    Integer newsletterId,
    Integer issueNo,
    LocalDateTime publishedAt,
    Boolean subscribed,

    @Nullable
    RevisionStateRes revision,

    @JsonProperty("snapshot")
    @Schema(name = "snapshot", description = "현재 상세 응답 기준 최신 snapshot")
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

package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RevisionOrOriginalDetailRes(
    Integer newsletterId,
    Integer issueNo,
    Long revisionId,
    String editStatus,
    Integer revisionNo,
    Boolean hasDraft,
    Integer latestDraftRevisionNo,
    @JsonProperty("snapshot")
    @Schema(name = "snapshot", description = "현재 응답 기준 최신 snapshot")
    ContentSnapshotRes latestSnapshot,
    Integer deltaCount,
    List<JsonPatchOpRes> deltaChanges
) {
}

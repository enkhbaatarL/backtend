package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record RevisionOrOriginalDetailRes(
    Integer newsletterId,
    Integer issueNo,
    Long revisionId,
    String editStatus,
    Integer revisionNo,
    String source,
    Boolean hasDraft,
    Integer latestDraftRevisionNo,
    JsonNode content,
    ContentSnapshotRes originalSnapshot,
    ContentSnapshotRes latestSnapshot,
    Integer deltaCount,
    List<JsonPatchOpRes> deltaChanges
) {
}

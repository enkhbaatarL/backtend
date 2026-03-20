package com.sk.domain.mi.dto.response.newsletter;

import java.util.List;

public record RevisionOrOriginalDetailRes(
    Integer newsletterId,
    Integer issueNo,
    Long revisionId,
    String editStatus,
    Integer revisionNo,
    Boolean hasDraft,
    Integer latestDraftRevisionNo,
    ContentSnapshotRes latestSnapshot,
    Integer deltaCount,
    List<JsonPatchOpRes> deltaChanges
) {
}

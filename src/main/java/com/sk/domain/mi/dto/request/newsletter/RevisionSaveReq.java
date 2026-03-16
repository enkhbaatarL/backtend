package com.sk.domain.mi.dto.request.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record RevisionSaveReq(
    Integer revisionNo,
    List<JsonPatchOpReq> deltas,
    JsonNode snapshotPayload
) {
}

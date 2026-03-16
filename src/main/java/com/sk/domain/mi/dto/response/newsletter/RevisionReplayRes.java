package com.sk.domain.mi.dto.response.newsletter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record RevisionReplayRes(
    BaseSnapshot baseSnapshot,
    Long fromSeq,
    Long toSeq,
    List<DeltaItemRes> histories
) {

    public record BaseSnapshot(
        Long seq,
        JsonNode data
    ) {
    }
}

package com.sk.domain.mi.dto.response.newsletter;

import java.util.List;

public record DeltaItemRes(
    Long seq,
    List<JsonPatchOpRes> deltas
) {
}

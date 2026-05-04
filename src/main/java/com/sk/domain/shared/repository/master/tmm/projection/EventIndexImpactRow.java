package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;

public record EventIndexImpactRow(
    Long miEventId,
    String targetId,
    String magnitudeLevel,
    String direction
) {

    @QueryProjection
    public EventIndexImpactRow {
    }

}

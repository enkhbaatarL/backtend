package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ImpactGrade;

public record EventIndexImpactRow(
    Long miEventId,
    String targetId,
    ImpactGrade impactShort,
    ImpactGrade impactMid,
    ImpactGrade impactLong
) {

    @QueryProjection
    public EventIndexImpactRow {
    }

}

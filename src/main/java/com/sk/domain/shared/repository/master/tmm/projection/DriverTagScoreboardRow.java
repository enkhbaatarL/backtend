package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.scenarioworkbench.ImpactGrade;

public record DriverTagScoreboardRow(
    String targetId,
    String lv1Id,
    String lv1Label,
    String lv2Id,
    String lv2Label,
    String lv3Id,
    String lv3Label,
    ImpactGrade impactShort,
    ImpactGrade impactMid,
    ImpactGrade impactLong,
    String driverReason
) {

    @QueryProjection
    public DriverTagScoreboardRow {
    }

}

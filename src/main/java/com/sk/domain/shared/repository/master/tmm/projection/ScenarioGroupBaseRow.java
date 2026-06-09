package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;

public record ScenarioGroupBaseRow(
    Long scenarioGroupId,
    Long eventTypeL2Id,
    String title,
    String summary,
    Integer scenarioCount,
    LocalDateTime createDt,
    String dimensionSelected
) {

    @QueryProjection
    public ScenarioGroupBaseRow {
    }

}

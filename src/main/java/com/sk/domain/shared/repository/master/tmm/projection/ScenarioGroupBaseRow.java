package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import java.time.LocalDateTime;

public record ScenarioGroupBaseRow(
    Long scenarioGroupId,
    Long eventTypeL2Id,
    String title,
    String summary,
    Integer scenarioCount,
    LocalDateTime createDt,
    LocalDateTime modifyDt,
    String dimensionSelected,
    ScenarioGroupStatus status
) {

    @QueryProjection
    public ScenarioGroupBaseRow {
    }

}

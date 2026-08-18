package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
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
    String dimensionDefinitions,
    ScenarioGroupStatus status,
    ScenarioGroupStage stage,
    Long requestUserId,
    String apiVersion
) {

    @QueryProjection
    public ScenarioGroupBaseRow {
    }

}

package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ScenarioApiVersion;
import java.time.LocalDateTime;

public record ScenarioGroupDetailBaseRow(
    Long scenarioGroupId,
    String groupName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String summary,
    Integer scenarioCount,
    String dimensionSelected,
    String dimensionDefinitions,
    Long eventTypeL2Id,
    ScenarioApiVersion apiVersion
) {

    @QueryProjection
    public ScenarioGroupDetailBaseRow {
    }

}

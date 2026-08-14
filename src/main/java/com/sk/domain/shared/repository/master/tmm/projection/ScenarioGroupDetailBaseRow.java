package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
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
    // base event
    Long eventId,
    String eventName,
    String eventTypeLv1Code,
    String eventTypeLv1Label,
    LocalDateTime eventUpdatedAt
) {

    @QueryProjection
    public ScenarioGroupDetailBaseRow {
    }

}

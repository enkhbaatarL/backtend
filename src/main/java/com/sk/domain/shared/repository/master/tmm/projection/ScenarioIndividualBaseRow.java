package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;


public record ScenarioIndividualBaseRow(
    Long scenarioId,
    String scenarioTitle,
    Boolean isBaseline,
    BigDecimal probabilityScore,
    ScenarioStatus status,
    String dimensionValues,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long scenarioGroupId,
    String groupTitle,
    LocalDateTime groupCreatedAt,
    String dimensionSelected,
    String dimensionDefinitions,
    Long eventTypeL2Id,
    Integer displayOrder
) {

    @QueryProjection
    public ScenarioIndividualBaseRow {
    }

}

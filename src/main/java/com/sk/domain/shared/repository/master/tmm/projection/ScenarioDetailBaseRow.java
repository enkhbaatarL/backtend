package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScenarioDetailBaseRow(
    Long scenarioId,
    Long scenarioGroupId,
    String title,
    Boolean isBaseline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String narrative,
    BigDecimal probabilityScore,
    String dimensionValues,
    String dimensionSelected,
    String dimensionDefinitions,
    Long eventTypeL2Id,
    Integer displayOrder,
    String scenarioRole
) {

    @QueryProjection
    public ScenarioDetailBaseRow {
    }

}

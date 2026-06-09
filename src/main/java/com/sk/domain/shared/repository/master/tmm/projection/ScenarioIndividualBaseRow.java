package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;


public record ScenarioIndividualBaseRow(
    Long scenarioId,
    String scenarioTitle,
    Boolean isBaseline,
    BigDecimal probabilityScore,
    String dimensionValues,
    Long scenarioGroupId,
    String groupTitle,
    LocalDateTime groupCreatedAt,
    String dimensionSelected,
    Long eventTypeL2Id,
    Integer displayOrder
) {

    @QueryProjection
    public ScenarioIndividualBaseRow {
    }

}

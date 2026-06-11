package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;

public record ScenarioBaseRow(
    Long scenarioGroupId,
    Long scenarioId,
    String title,
    Boolean isBaseline,
    BigDecimal probabilityScore,
    String status,
    String dimensionValues,
    Integer displayOrder
) {

    @QueryProjection
    public ScenarioBaseRow {
    }

}

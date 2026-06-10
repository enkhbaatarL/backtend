package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;

public record ScenarioDetailBaseRow(
    Long scenarioId,
    Long scenarioGroupId,
    String title,
    Boolean isBaseline,
    String narrative,
    BigDecimal probabilityScore,
    String dimensionValues,
    String dimensionSelected,
    Long eventTypeL2Id
) {

    @QueryProjection
    public ScenarioDetailBaseRow {
    }

}

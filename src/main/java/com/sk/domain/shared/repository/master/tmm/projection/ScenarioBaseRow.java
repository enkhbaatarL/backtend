package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import java.math.BigDecimal;

public record ScenarioBaseRow(
    Long scenarioGroupId,
    Long scenarioId,
    String title,
    Boolean isBaseline,
    BigDecimal probabilityScore,
    ScenarioStatus status,
    String dimensionValues,
    Integer displayOrder,
    String scenarioRole
) {

    @QueryProjection
    public ScenarioBaseRow {
    }

}

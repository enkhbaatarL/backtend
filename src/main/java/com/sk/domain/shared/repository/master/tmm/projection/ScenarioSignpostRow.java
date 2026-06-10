package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;
import java.math.BigDecimal;

public record ScenarioSignpostRow(
    String label,
    ProbabilityDirection probabilityDirection,
    BigDecimal probabilityImpactScore,
    String signpostReason
) {

    @QueryProjection
    public ScenarioSignpostRow {
    }

}

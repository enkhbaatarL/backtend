package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;
import com.sk.domain.shared.enums.nextgen.SignpostStrength;
import java.math.BigDecimal;

public record ScenarioSignpostRow(
    Long signpostId,
    String label,
    ProbabilityDirection probabilityDirection,
    BigDecimal probabilityImpactScore,
    SignpostStrength strength,
    String signpostReason,
    Integer displayOrder
) {

    @QueryProjection
    public ScenarioSignpostRow {
    }

}

package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScenarioSignpostEventRow(
    LocalDateTime occurredAt,
    String summary,
    BigDecimal probabilityScore,
    BigDecimal probabilityDelta,
    String matchingReason
) {

    @QueryProjection
    public ScenarioSignpostEventRow {
    }

}

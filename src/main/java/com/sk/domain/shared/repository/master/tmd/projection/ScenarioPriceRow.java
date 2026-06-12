package com.sk.domain.shared.repository.master.tmd.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;

/**
 * 시나리오 그래프의 한 점
 */
public record ScenarioPriceRow(
    Long scenarioId,
    String periodKey,
    BigDecimal baseCasePrice,
    BigDecimal userAdjustedPrice
) {

    @QueryProjection
    public ScenarioPriceRow {
    }

}

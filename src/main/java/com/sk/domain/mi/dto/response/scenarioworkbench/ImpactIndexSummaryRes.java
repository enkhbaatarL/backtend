package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;

public record ImpactIndexSummaryRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "시장 영향도")
    MarketImpactLevel impactLevel,

    @Schema(description = "가격 방향")
    MarketDirection priceDirection

) {
}

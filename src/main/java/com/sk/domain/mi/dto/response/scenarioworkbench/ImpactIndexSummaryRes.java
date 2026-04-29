package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

public record ImpactIndexSummaryRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "영향도 수준", example = "high")
    MarketImpactLevel impactLevel,

    @Schema(description = "영향도 수준 라벨", example = "높음")
    String impactLevelLabel,

    @Schema(description = "가격 방향", example = "bullish")
    MarketDirection direction,

    @Schema(description = "가격 방향 라벨", example = "상승")
    String directionLabel

) {

    public static ImpactIndexSummaryRes from(EventIndexImpactRow row) {
        MarketImpactLevel impactLevel = MarketImpactLevel.fromCode(row.magnitudeLevel());
        MarketDirection direction = MarketDirection.fromCode(row.direction());

        return new ImpactIndexSummaryRes(
            row.targetId(),
            impactLevel,
            Optional.ofNullable(impactLevel).map(MarketImpactLevel::getLabel).orElse(null),
            direction,
            Optional.ofNullable(direction).map(MarketDirection::getLabel).orElse(null)
        );
    }

}

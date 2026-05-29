package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

public record EventImpactIndexTermRes(

    @Schema(description = "시장 영향도 수준", example = "high")
    MarketImpactLevel marketImpactLevel,

    @Schema(description = "시장 영향도 수준 라벨", example = "높음")
    String marketImpactLevelLabel,

    @Schema(description = "가격 방향", example = "bullish")
    MarketDirection direction,

    @Schema(description = "가격 방향 라벨", example = "상승")
    String directionLabel

) {

    public static EventImpactIndexTermRes of(String magnitudeLevel, String directionCode) {
        MarketImpactLevel level = MarketImpactLevel.fromCode(magnitudeLevel);
        MarketDirection dir = MarketDirection.fromCode(directionCode);

        return new EventImpactIndexTermRes(
            level,
            Optional.ofNullable(level).map(MarketImpactLevel::getLabel).orElse(null),
            dir,
            Optional.ofNullable(dir).map(MarketDirection::getLabel).orElse(null)
        );
    }

}

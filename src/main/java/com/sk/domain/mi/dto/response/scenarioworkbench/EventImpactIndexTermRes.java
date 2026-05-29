package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ImpactGrade;
import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;

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

    // ImpactGrade(상승_상 등) → {방향, 강도}로 분해. grade가 null이면 해당 시점 미산출.
    public static EventImpactIndexTermRes of(ImpactGrade grade) {
        if (grade == null) {
            return null;
        }
        MarketImpactLevel level = grade.getLevel();
        MarketDirection direction = grade.getDirection();
        return new EventImpactIndexTermRes(
            level,
            level == null ? null : level.getLabel(),
            direction,
            direction == null ? null : direction.getLabel()
        );
    }

}

package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.SignpostImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;

public record ScenarioSignpostDefinitionRes(

    @Schema(description = "분류", example = "Observation")
    String category,

    @Schema(description = "Signpost 라벨", example = "일본 JERA, 여름 피크 대비 추가 LNG 카고 10선 긴급 조달 발표")
    String label,

    @Schema(description = "방향", example = "bullish")
    MarketDirection direction,

    @Schema(description = "방향 라벨", example = "상승")
    String directionLabel,

    @Schema(description = "Signpost 영향도", example = "strong")
    SignpostImpactLevel signpostImpactLevel,

    @Schema(description = "Signpost 영향도 라벨", example = "강함")
    String signpostImpactLevelLabel,

    @Schema(description = "핵심 근거")
    String rationale

) {
}

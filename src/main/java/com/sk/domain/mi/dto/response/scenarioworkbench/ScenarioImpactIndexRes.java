package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;

public record ScenarioImpactIndexRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "시장 영향도 수준", example = "high")
    MarketImpactLevel marketImpactLevel,

    @Schema(description = "시장 영향도 수준 라벨", example = "높음")
    String marketImpactLevelLabel

) {}

package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ScenarioIndexOutlookRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "영향도 수준", example = "high")
    MarketImpactLevel impactLevel,

    @Schema(description = "영향도 수준 라벨", example = "높음")
    String impactLevelLabel,

    @Schema(description = "가격 방향", example = "bullish")
    MarketDirection direction,

    @Schema(description = "가격 방향 라벨", example = "상승")
    String directionLabel,

    @Schema(description = "가격 흐름 (구간별 가격 범위)")
    List<PriceFlowStep> priceFlow,

    @Schema(description = "경향 전망 본문")
    String trendText,

    @Schema(description = "경향 전망 요약", example = "4~6월 $21~23 (급등 충격) → 7~9월 $19~20 (수요 피크+증산) → 4Q $18.5~19.5 (거래 반등)")
    String trendSummary

) {

    public record PriceFlowStep(
        @Schema(description = "가격 하한 (단일값이면 from == to)", example = "21")
        BigDecimal from,

        @Schema(description = "가격 상한 (단일값이면 from == to)", example = "23")
        BigDecimal to
    ) {}

}

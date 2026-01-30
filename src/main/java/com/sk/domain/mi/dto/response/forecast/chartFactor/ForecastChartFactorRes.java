package com.sk.domain.mi.dto.response.forecast.chartFactor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

public record ForecastChartFactorRes(
        @Schema(description = "카테고리")
        String explanatoryLevelNm,

        @Schema(description = "예측 요인 설명")
        String categorySummary,

        @Schema(description = "플러스 요인")
        List<ForecastChartFactor> plusFactors,

        @Schema(description = "마이너스 요인")
        List<ForecastChartFactor> minusFactors) {
    @Builder
    public ForecastChartFactorRes(String explanatoryLevelNm, String categorySummary,
            List<ForecastChartFactor> plusFactors, List<ForecastChartFactor> minusFactors) {
        this.explanatoryLevelNm = explanatoryLevelNm;
        this.categorySummary = categorySummary;
        this.plusFactors = plusFactors;
        this.minusFactors = minusFactors;
    }

    public static ForecastChartFactorRes of(String explanatoryLevelNm, String categorySummary,
            List<ForecastChartFactor> plusFactors, List<ForecastChartFactor> minusFactors) {
        return ForecastChartFactorRes.builder()
                .explanatoryLevelNm(explanatoryLevelNm)
                .categorySummary(categorySummary)
                .plusFactors(plusFactors)
                .minusFactors(minusFactors)
                .build();
    }
}

package com.sk.domain.mi.dto.response.dashboard.charts;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartFactorRes(
        @Schema(description = "카테고리")
        String explanatoryLevelNm,

        @Schema(description = "예측 요인 설명")
        String summary,

        @Schema(description = "플러스 요인")
        List<DashboardChartFactor> plusFactors,

        @Schema(description = "마이너스 요인")
        List<DashboardChartFactor> minusFactors) {
    @Builder
    public DashboardChartFactorRes(String explanatoryLevelNm, String summary, List<DashboardChartFactor> plusFactors, List<DashboardChartFactor> minusFactors) {
        this.explanatoryLevelNm = explanatoryLevelNm;
        this.summary = summary;
        this.plusFactors = plusFactors;
        this.minusFactors = minusFactors;
    }

    public static DashboardChartFactorRes of(String explanatoryLevelNm, List<DashboardChartFactor> plusFactors, List<DashboardChartFactor> minusFactors) {
        return DashboardChartFactorRes.builder()
                .explanatoryLevelNm(explanatoryLevelNm)
                .plusFactors(plusFactors)
                .minusFactors(minusFactors)
                .build();
    }
}

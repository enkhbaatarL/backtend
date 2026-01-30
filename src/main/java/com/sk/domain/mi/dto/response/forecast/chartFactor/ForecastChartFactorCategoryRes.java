package com.sk.domain.mi.dto.response.forecast.chartFactor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

public record ForecastChartFactorCategoryRes(
    @Schema(description = "카테고리 목록") List<String> explanatoryLevelNms
) {
    @Builder
    public ForecastChartFactorCategoryRes(List<String> explanatoryLevelNms) {
        this.explanatoryLevelNms = explanatoryLevelNms;
    }

    public static ForecastChartFactorCategoryRes of(List<String> explanatoryLevelNms) {
        return ForecastChartFactorCategoryRes.builder()
            .explanatoryLevelNms(explanatoryLevelNms)
            .build();
    }
}

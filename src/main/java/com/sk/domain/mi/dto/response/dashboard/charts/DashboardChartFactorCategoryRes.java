package com.sk.domain.mi.dto.response.dashboard.charts;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
public record DashboardChartFactorCategoryRes(
    @Schema(description = "카테고리 목록") List<String> explanatoryLevel
) {
    @Builder
    public DashboardChartFactorCategoryRes(List<String> explanatoryLevel) {
        this.explanatoryLevel = explanatoryLevel;
    }

    public static DashboardChartFactorCategoryRes of(List<String> explanatoryLevel) {
        return DashboardChartFactorCategoryRes.builder()
            .explanatoryLevel(explanatoryLevel)
            .build();
    }
}

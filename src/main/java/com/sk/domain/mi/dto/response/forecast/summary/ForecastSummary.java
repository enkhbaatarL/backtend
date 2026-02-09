package com.sk.domain.mi.dto.response.forecast.summary;

import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastSummary(
    @Schema(description = "설명력제목내용") String explanatoryTitleContent,

    @Schema(description = "설명력요약내용") String explanatorySummaryContent,

    @Schema(description = "설명력내용파일경로명") String explanatoryFilePath) {

    @Builder
    public ForecastSummary(String explanatoryTitleContent, String explanatorySummaryContent,
        String explanatoryFilePath) {
        this.explanatoryTitleContent = explanatoryTitleContent;
        this.explanatorySummaryContent = explanatorySummaryContent;
        this.explanatoryFilePath = explanatoryFilePath;
    }

    public static ForecastSummary of(DashboardChartSummaryMap dashboardChartSummaryMap) {
        return ForecastSummary.builder()
            .explanatoryTitleContent(dashboardChartSummaryMap.getExplanatoryTitleContent())
            .explanatorySummaryContent(dashboardChartSummaryMap.getExplanatorySummaryContent())
            .explanatoryFilePath(dashboardChartSummaryMap.getExplanatoryFilePath())
            .build();
    }
}

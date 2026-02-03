package com.sk.domain.mi.dto.response.forecast.report;

import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastReport(
        @Schema(description = "설명력제목내용") String explanatoryTitleContent,
        @Schema(description = "설명력요약내용") String explanatorySummaryContent,
        @Schema(description = "설명력내용파일경로명") String explanatoryFilePath) {

    @Builder
    public ForecastReport(String explanatoryTitleContent, String explanatorySummaryContent,
            String explanatoryFilePath) {
        this.explanatoryTitleContent = explanatoryTitleContent;
        this.explanatorySummaryContent = explanatorySummaryContent;
        this.explanatoryFilePath = explanatoryFilePath;
    }

    public static ForecastReport of(DashboardChartSummaryMap summary) {
        return ForecastReport.builder()
                .explanatoryTitleContent(summary.getExplanatoryTitleContent())
                .explanatorySummaryContent(summary.getExplanatorySummaryContent())
                .explanatoryFilePath(summary.getExplanatoryFilePath())
                .build();
    }
}

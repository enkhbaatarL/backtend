package com.sk.domain.mi.dto.response.dashboard.summary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardSummary(
        @Schema(description = "설명력제목내용") String explanatoryTitleContent,

        @Schema(description = "설명력요약내용") String explanatorySummaryContent,

        @Schema(description = "설명력내용파일경로명") String explanatoryFilePath) {

    @Builder
    public DashboardSummary(String explanatoryTitleContent, String explanatorySummaryContent,
            String explanatoryFilePath) {
        this.explanatoryTitleContent = explanatoryTitleContent;
        this.explanatorySummaryContent = explanatorySummaryContent;
        this.explanatoryFilePath = explanatoryFilePath;
    }

    public static DashboardSummary of(DashboardChartSummaryMap dashboardChartSummaryMap) {
        return DashboardSummary.builder()
                .explanatoryTitleContent(dashboardChartSummaryMap.getExplanatoryTitleContent())
                .explanatorySummaryContent(dashboardChartSummaryMap.getExplanatorySummaryContent())
                .explanatoryFilePath(dashboardChartSummaryMap.getExplanatoryFilePath())
                .build();
    }
}

package com.sk.domain.mi.dto.response.dashboard.charts.graph;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.enums.chart.ChartPredictLineType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChart(
        @Schema(description = "차트 라인 설명", example = "IONIQ 5") String chartLineDescription,
        @Schema(description = "차트 예측선 타입", example = "LINE") ChartPredictLineType chartLineType,
        @Schema(description = "Actual 데이터") DashboardChartLine actualChartLines,
        @Schema(description = "Forecast 데이터") DashboardChartLine forecastChartLines) {
    @Builder
    public DashboardChart(String chartLineDescription, ChartPredictLineType chartLineType,
            DashboardChartLine actualChartLines, DashboardChartLine forecastChartLines) {
        this.chartLineDescription = chartLineDescription;
        this.chartLineType = chartLineType;
        this.actualChartLines = actualChartLines;
        this.forecastChartLines = forecastChartLines;
    }

    public static DashboardChart of(ChartPredictSettingDetail chartPredictSettingDetail,
            DashboardChartLine actualChartLines, DashboardChartLine forecastChartLines) {
            return DashboardChart.builder()
                    .chartLineDescription(chartPredictSettingDetail.getPredictChartDetailExpn())
                    .chartLineType(chartPredictSettingDetail.getChartPredictLineSeparateCd())
                    .actualChartLines(actualChartLines)
                    .forecastChartLines(forecastChartLines)
                    .build();
    }
}

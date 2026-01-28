package com.sk.domain.mi.dto.response.forecast.charts.graph;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.enums.chart.ChartPredictLineType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastChart(
        @Schema(description = "차트 라인 설명", example = "IONIQ 5") String chartLineDescription,
        @Schema(description = "차트 예측선 타입", example = "LINE") ChartPredictLineType chartLineType,
        @Schema(description = "Actual 데이터") ForecastChartLine actualChartLines,
        @Schema(description = "Forecast 데이터") ForecastChartLine forecastChartLines) {
    @Builder
    public ForecastChart(String chartLineDescription, ChartPredictLineType chartLineType,
            ForecastChartLine actualChartLines, ForecastChartLine forecastChartLines) {
        this.chartLineDescription = chartLineDescription;
        this.chartLineType = chartLineType;
        this.actualChartLines = actualChartLines;
        this.forecastChartLines = forecastChartLines;
    }

    public static ForecastChart of(ChartPredictSettingDetail chartPredictSettingDetail,
            ForecastChartLine actualChartLines, ForecastChartLine forecastChartLines) {
        return ForecastChart.builder()
                .chartLineDescription(chartPredictSettingDetail.getPredictChartDetailExpn())
                .chartLineType(chartPredictSettingDetail.getChartPredictLineSeparateCd())
                .actualChartLines(actualChartLines)
                .forecastChartLines(forecastChartLines)
                .build();
    }
}

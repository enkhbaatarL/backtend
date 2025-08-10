package com.sk.domain.mi.dto.response.dashboard.charts.graph;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartRawData(
        @Schema(description = "예측 결과 월(YYYY-MM)", example = "2024-05") String resultDate,
        @Schema(description = "예측 수행 일자(yyyyMMdd)", example = "20240501") String predictDate,
        @Schema(description = "월별 평균 데이터 값", example = "123.45") Double value) {
    @Builder
    public DashboardChartRawData(String resultDate, String predictDate, Double value) {
        this.resultDate = resultDate;
        this.predictDate = predictDate;
        this.value = value;
    }

    public static DashboardChartRawData ofActual(String resultDate, Double value) {
        return DashboardChartRawData.builder()
                .resultDate(resultDate)
                .value(value)
                .build();
    }

    public static DashboardChartRawData ofForecast(String resultDate, String predictDate, Double value) {
        return DashboardChartRawData.builder()
                .resultDate(resultDate)
                .predictDate(predictDate)
                .value(value)
                .build();
    }
}

package com.sk.domain.mi.dto.response.dashboard.charts.graph;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartRawData(
        @Schema(description = "날짜 (YYYY-MM)", example = "2024-05") String date,
        @Schema(description = "월별 평균 데이터 값", example = "123.45") Double value) {
    @Builder
    public DashboardChartRawData(String date, Double value) {
        this.date = date;
        this.value = value;
    }

    public static DashboardChartRawData of(String date, Double value) {
        return DashboardChartRawData.builder()
                .date(date)
                .value(value)
                .build();
    }
}

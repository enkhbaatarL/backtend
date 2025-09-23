package com.sk.domain.mi.dto.response.marketData.charts.graph;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MarketDataChartRawData(
    @Schema(description = "월별 대표 날짜 (YYYYMMDD)", example = "20240501") String date,
    @Schema(description = "월별 평균 데이터 값", example = "123.45") Double value,
    @Schema(description = "월별 평균 데이터 값(실제), null일 경우 value 가 실제 값", example = "123.45") Double real
) {

    @Builder
    public MarketDataChartRawData(String date, Double value, Double real) {
        this.date = date;
        this.value = value;
        this.real = real;
    }

    public static MarketDataChartRawData of(String date, Double value) {
        return MarketDataChartRawData.builder()
            .date(date)
            .value(value)
            .build();
    }
    public static MarketDataChartRawData of(String date, Double value, Double real) {
        return MarketDataChartRawData.builder()
            .date(date)
            .value(value)
            .real(real)
            .build();
    }
}

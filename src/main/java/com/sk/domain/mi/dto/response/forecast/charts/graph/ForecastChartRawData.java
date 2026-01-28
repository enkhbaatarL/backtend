package com.sk.domain.mi.dto.response.forecast.charts.graph;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sk.common.converter.DoubleTwoScaleSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastChartRawData(
        @Schema(description = "예측 결과 월(YYYY-MM)", example = "2024-05") String resultDate,
        @Schema(description = "예측 수행 일자(yyyyMMdd)", example = "20240501") String predictDate,
        @Schema(description = "월별 평균 데이터 값", example = "123.45")
        @JsonSerialize(using = DoubleTwoScaleSerializer.class)
        Double value) {
    @Builder
    public ForecastChartRawData(String resultDate, String predictDate, Double value) {
        this.resultDate = resultDate;
        this.predictDate = predictDate;
        this.value = value;
    }

    public static ForecastChartRawData ofActual(String resultDate, Double value) {
        return ForecastChartRawData.builder()
                .resultDate(resultDate)
                .value(value)
                .build();
    }

    public static ForecastChartRawData ofForecast(String resultDate, String predictDate, Double value) {
        return ForecastChartRawData.builder()
                .resultDate(resultDate)
                .predictDate(predictDate)
                .value(value)
                .build();
    }
}

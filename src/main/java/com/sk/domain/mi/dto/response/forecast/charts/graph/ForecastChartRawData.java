package com.sk.domain.mi.dto.response.forecast.charts.graph;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sk.common.converter.ForecastChartValueScaleSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastChartRawData(
        @Schema(description = "결과 라벨(단위별 표시값)", example = "2024-05") String resultDate,
        @Schema(description = "결과 기간 시작일자(yyyyMMdd)", example = "20240501") String startDate,
        @Schema(description = "예측 수행 일자(yyyyMMdd)", example = "20240501") String predictDate,
        @Schema(description = "기간 평균 데이터 값", example = "123.45")
        @JsonSerialize(using = ForecastChartValueScaleSerializer.class)
        Double value) {
    @Builder
    public ForecastChartRawData(String resultDate, String startDate, String predictDate,
            Double value) {
        this.resultDate = resultDate;
        this.startDate = startDate;
        this.predictDate = predictDate;
        this.value = value;
    }

    public static ForecastChartRawData ofActual(String resultDate, Double value) {
        return ForecastChartRawData.builder()
                .resultDate(resultDate)
                .startDate(null)
                .value(value)
                .build();
    }

    public static ForecastChartRawData ofForecast(String resultDate, String predictDate, Double value) {
        return ForecastChartRawData.builder()
                .resultDate(resultDate)
                .startDate(null)
                .predictDate(predictDate)
                .value(value)
                .build();
    }

    public static ForecastChartRawData ofActual(String resultDate, String startDate, Double value) {
        return ForecastChartRawData.builder()
                .resultDate(resultDate)
                .startDate(startDate)
                .value(value)
                .build();
    }

    public static ForecastChartRawData ofForecast(String resultDate, String startDate,
            String predictDate, Double value) {
        return ForecastChartRawData.builder()
                .resultDate(resultDate)
                .startDate(startDate)
                .predictDate(predictDate)
                .value(value)
                .build();
    }
}

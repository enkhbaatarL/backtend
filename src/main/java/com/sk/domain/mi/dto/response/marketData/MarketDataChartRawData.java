package com.sk.domain.mi.dto.response.marketData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MarketDataChartRawData(
    @Schema(description = "월별 대표 날짜 (YYYYMMDD)", example = "20240501") String date,
    @Schema(description = "월별 평균 데이터 값", example = "123.45") Double value
) {
    public static MarketDataChartRawData of(String date, Double value) {
        return MarketDataChartRawData.builder()
            .date(date)
            .value(value)
            .build();
    }
}

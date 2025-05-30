package com.sk.domain.mi.dto.response.marketData.charts.graph;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MarketDataChart(
        @Schema(description = "그래프 데이터") List<MarketDataChartRawData> rawData) {
    @Builder
    public MarketDataChart(List<MarketDataChartRawData> rawData) {
        this.rawData = rawData;
    }

    public static MarketDataChart of(List<MarketDataChartRawData> rawData) {
        return MarketDataChart.builder()
                .rawData(rawData)
                .build();
    }
}

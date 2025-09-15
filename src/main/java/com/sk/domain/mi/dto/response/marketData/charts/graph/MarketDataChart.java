package com.sk.domain.mi.dto.response.marketData.charts.graph;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MarketDataChart(
        @Schema(description = "그래프 데이터") List<MarketDataChartRawData> rawData,
        @Schema(description = "부가차트 이름") String specificIndicatorName
        ) {
    @Builder
    public MarketDataChart(List<MarketDataChartRawData> rawData, String specificIndicatorName) {
        this.rawData = rawData;
        this.specificIndicatorName = specificIndicatorName;
    }

    public static MarketDataChart of(List<MarketDataChartRawData> rawData) {
        return MarketDataChart.builder()
            .rawData(rawData)
            .build();
    }

    public static MarketDataChart of(List<MarketDataChartRawData> rawData, String specificIndicatorName) {
        return MarketDataChart.builder()
                .rawData(rawData)
                .specificIndicatorName(specificIndicatorName)
                .build();
    }

}

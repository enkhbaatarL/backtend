package com.sk.domain.mi.dto.response.marketData.charts.rawData;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
public record MarketDataChartRes(
        @Schema(description = "그래프 데이터") List<MarketDataChartRawData> rawData) {
            

    @Builder
    public MarketDataChartRes(List<MarketDataChartRawData> rawData) {
        this.rawData = rawData;
    }

    public static MarketDataChartRes of(List<MarketDataChartRawData> rawData) {
        return MarketDataChartRes.builder()
            .rawData(rawData)
            .build();
    }
}

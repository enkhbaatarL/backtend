package com.sk.domain.mi.dto.response.marketData;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record MarketDataChartRes(
        @Schema(description = "그래프 데이터") List<MarketDataChartRawData> rawData) {
            
    public static MarketDataChartRes of(List<MarketDataChartRawData> rawData) {
        return new MarketDataChartRes(rawData);
    }
}

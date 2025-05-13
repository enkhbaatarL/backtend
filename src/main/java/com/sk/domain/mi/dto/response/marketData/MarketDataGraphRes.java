package com.sk.domain.mi.dto.response.marketData;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record MarketDataGraphRes(
        @Schema(description = "그래프 데이터") List<MarketDataGraphRawData> rawData) {
            
    public static MarketDataGraphRes of(List<MarketDataGraphRawData> rawData) {
        return new MarketDataGraphRes(rawData);
    }
}

package com.sk.domain.mi.dto.response.marketData;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;

public record MiMarketDataCache(
        MarketMetaData marketMetaData,
        MarketDataChartRes mainChart,
        List<MarketDataChartRes> compareCharts) {

    @Builder
    @JsonCreator
    public MiMarketDataCache(
            MarketMetaData marketMetaData,
            MarketDataChartRes mainChart,
            List<MarketDataChartRes> compareCharts) {
        this.marketMetaData = marketMetaData;
        this.mainChart = mainChart;
        this.compareCharts = compareCharts;
    }

    public static MiMarketDataCache of(MarketMetaData marketMetaData, MarketDataChartRes mainChart,
            List<MarketDataChartRes> compareCharts) {
        return MiMarketDataCache.builder()
                .marketMetaData(marketMetaData)
                .mainChart(mainChart)
                .compareCharts(compareCharts)
                .build();
    }
}

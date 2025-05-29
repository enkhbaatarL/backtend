package com.sk.domain.mi.dto.response.marketData.charts;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChart;
import com.sk.domain.mi.dto.response.marketData.charts.metaData.MarketMetaData;

import lombok.Builder;

public record MiMarketDataCache(
        MarketMetaData marketMetaData,
        MarketDataChart mainChart,
        List<MarketDataChart> compareCharts) {

    @Builder
    @JsonCreator
    public MiMarketDataCache(
            MarketMetaData marketMetaData,
            MarketDataChart mainChart,
            List<MarketDataChart> compareCharts) {
        this.marketMetaData = marketMetaData;
        this.mainChart = mainChart;
        this.compareCharts = compareCharts;
    }

    public static MiMarketDataCache of(MarketMetaData marketMetaData, MarketDataChart mainChart,
            List<MarketDataChart> compareCharts) {
        return MiMarketDataCache.builder()
                .marketMetaData(marketMetaData)
                .mainChart(mainChart)
                .compareCharts(compareCharts)
                .build();
    }
}

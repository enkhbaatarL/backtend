package com.sk.domain.mi.dto.response.marketData.charts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sk.domain.mi.dto.response.marketData.charts.metaData.MarketMetaDataSummary;

import lombok.Builder;

public record MiMarketDataSummaryCache(
        MarketMetaDataSummary marketMetaDataSummary

) {

    @Builder
    @JsonCreator
    public MiMarketDataSummaryCache(MarketMetaDataSummary marketMetaDataSummary) {
        this.marketMetaDataSummary = marketMetaDataSummary;
    }

    public static MiMarketDataSummaryCache of(MarketMetaDataSummary marketMetaDataSummary) {
        return MiMarketDataSummaryCache.builder()
                .marketMetaDataSummary(marketMetaDataSummary)
                .build();
    }
}

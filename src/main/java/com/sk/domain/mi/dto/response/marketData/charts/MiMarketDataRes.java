package com.sk.domain.mi.dto.response.marketData.charts;

import java.util.List;

import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChart;
import com.sk.domain.mi.dto.response.marketData.charts.metaData.MarketMetaData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiMarketDataRes(
        @Schema(description = "권한 여부", example = "true") Boolean isPermitted,

        @Schema(description = "메타 데이터") MarketMetaData marketMetaData,

        @Schema(description = "메인 그래프") MarketDataChart mainChart,

        @Schema(description = "비교 그래프") List<MarketDataChart> compareCharts
) {


    @Builder
    public MiMarketDataRes(Boolean isPermitted, MarketMetaData marketMetaData, MarketDataChart mainChart,
            List<MarketDataChart> compareCharts) {
        this.isPermitted = isPermitted;
        this.marketMetaData = marketMetaData;
        this.mainChart = mainChart;
        this.compareCharts = compareCharts;
    }

    public static MiMarketDataRes of(Boolean isPermitted, MarketMetaData marketMetaData) {
        return MiMarketDataRes.builder()
                .isPermitted(isPermitted)
                .marketMetaData(marketMetaData)
                .build();
    }

    public static MiMarketDataRes of(Boolean isPermitted, MarketMetaData marketMetaData, MarketDataChart mainChart,
            List<MarketDataChart> compareCharts) {
        return MiMarketDataRes.builder()
                .isPermitted(isPermitted)
                .marketMetaData(marketMetaData)
                .mainChart(mainChart)
                .compareCharts(compareCharts)
                .build();
    }
}

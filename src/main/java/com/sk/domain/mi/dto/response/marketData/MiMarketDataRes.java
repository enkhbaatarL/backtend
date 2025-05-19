package com.sk.domain.mi.dto.response.marketData;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MiMarketDataRes(

        @Schema(description = "메인 그래프") MarketDataChartRes mainChart,

        @Schema(description = "권한 여부", example = "true") Boolean isPermitted,

        @Schema(description = "메타 데이터") MarketMetaData marketMetaData,

        @Schema(description = "비교 그래프") List<MarketDataChartRes> compareCharts) {

    public static MiMarketDataRes of(Boolean isPermitted, MarketMetaData marketMetaData) {
        return MiMarketDataRes.builder()
                .isPermitted(isPermitted)
                .marketMetaData(marketMetaData)
                .build();
    }

    public static MiMarketDataRes of(Boolean isPermitted, MarketMetaData marketMetaData, MarketDataChartRes mainChart,
            List<MarketDataChartRes> compareCharts) {
        return MiMarketDataRes.builder()
                .isPermitted(isPermitted)
                .marketMetaData(marketMetaData)
                .mainChart(mainChart)
                .compareCharts(compareCharts)
                .build();
    }
}

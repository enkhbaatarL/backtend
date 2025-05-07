package com.sk.domain.mi.dto.response.marketData;

import com.sk.domain.mi.dto.response.charts.Chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MiMarketDataRes(

        @Schema(description = "권한 여부", example = "true") 
        Boolean isPermitted,

        @Schema(description = "메인 차트")
        Chart mainChart,

        @Schema(description = "메타 데이터")
        MarketMetaData marketMetaData
) 
{

    @Builder
    public MiMarketDataRes(Boolean isPermitted, Chart mainChart, MarketMetaData marketMetaData) {
        this.isPermitted = isPermitted;
        this.mainChart = mainChart;
        this.marketMetaData = marketMetaData;
    }

    public static MiMarketDataRes of(Boolean isPermitted) {
        return MiMarketDataRes.builder()
            .isPermitted(isPermitted)
            .build();
    }

    public static MiMarketDataRes of(Boolean isPermitted, Chart mainChart, MarketMetaData marketMetaData) {
        return MiMarketDataRes.builder()
            .isPermitted(isPermitted)
            .mainChart(mainChart)
            .marketMetaData(marketMetaData)
            .build();
    }

}

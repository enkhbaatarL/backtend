package com.sk.domain.mi.dto.response.marketData;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MiMarketDataRes(

        @Schema(description = "메인 그래프") MarketDataGraphRes mainGraph,

        @Schema(description = "권한 여부", example = "true") Boolean isPermitted,

        @Schema(description = "메타 데이터") MarketMetaData marketMetaData,

        @Schema(description = "비교 그래프") List<MarketDataGraphRes> compareGraphs) {

    public static MiMarketDataRes of(Boolean isPermitted) {
        return MiMarketDataRes.builder()
                .isPermitted(isPermitted)
                .build();
    }

    public static MiMarketDataRes of(Boolean isPermitted, MarketMetaData marketMetaData, MarketDataGraphRes mainGraph, 
            List<MarketDataGraphRes> compareGraphs) {
        return MiMarketDataRes.builder()
                .isPermitted(isPermitted)
                .marketMetaData(marketMetaData)
                .mainGraph(mainGraph)
                .compareGraphs(compareGraphs)
                .build();
    }
}

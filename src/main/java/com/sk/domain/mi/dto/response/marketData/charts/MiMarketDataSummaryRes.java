package com.sk.domain.mi.dto.response.marketData.charts;

import com.sk.domain.mi.dto.response.marketData.charts.metaData.MarketMetaDataSummary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiMarketDataSummaryRes(

        @Schema(description = "권한 여부", example = "true") Boolean isPermitted,

        @Schema(description = "메타 데이터") MarketMetaDataSummary marketMetaDataSummary

) {

    @Builder
    public MiMarketDataSummaryRes(Boolean isPermitted, MarketMetaDataSummary marketMetaDataSummary) {
        this.isPermitted = isPermitted;
        this.marketMetaDataSummary = marketMetaDataSummary;
    }

    public static MiMarketDataSummaryRes of(Boolean isPermitted, MarketMetaDataSummary marketMetaDataSummary) {
        return MiMarketDataSummaryRes.builder()
                .isPermitted(isPermitted)
                .marketMetaDataSummary(marketMetaDataSummary)
                .build();
    }

}

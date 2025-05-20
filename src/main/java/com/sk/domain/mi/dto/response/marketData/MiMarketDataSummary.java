package com.sk.domain.mi.dto.response.marketData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiMarketDataSummary(

        @Schema(description = "권한 여부", example = "true") Boolean isPermitted,

        @Schema(description = "메타 데이터") MarketMetaDataSummary marketMetaDataSummary

) {

    @Builder
    public MiMarketDataSummary(Boolean isPermitted, MarketMetaDataSummary marketMetaDataSummary) {
        this.isPermitted = isPermitted;
        this.marketMetaDataSummary = marketMetaDataSummary;
    }

    public static MiMarketDataSummary of(Boolean isPermitted, MarketMetaDataSummary marketMetaDataSummary) {
        return MiMarketDataSummary.builder()
                .isPermitted(isPermitted)
                .marketMetaDataSummary(marketMetaDataSummary)
                .build();
    }

}

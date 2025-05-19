package com.sk.domain.mi.dto.response.marketData;

import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChangeRatesRes(
    @Schema(description = "주간 변화율", example = "1.0") Double wowChangeRate,
    @Schema(description = "월간 변화율", example = "1.0") Double momChangeRate,
    @Schema(description = "년간 변화율", example = "1.0") Double yoyChangeRate
) {

    @Builder
    public ChangeRatesRes(Double wowChangeRate, Double momChangeRate, Double yoyChangeRate) {
        this.wowChangeRate = wowChangeRate;
        this.momChangeRate = momChangeRate;
        this.yoyChangeRate = yoyChangeRate;
    }

    public static ChangeRatesRes of(Double wowChangeRate, Double momChangeRate, Double yoyChangeRate) {
        return ChangeRatesRes.builder()
            .wowChangeRate(wowChangeRate)
            .momChangeRate(momChangeRate)
            .yoyChangeRate(yoyChangeRate)
            .build();
    }
}


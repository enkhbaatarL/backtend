package com.sk.domain.mi.dto.response.marketData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
public record CalculatePeriodChangeValue(
        @Schema(description = "변화율", example = "1.0") Double changeRate,
        @Schema(description = "변화 수치", example = "1.0") Double changeValue) {

    @Builder
    public CalculatePeriodChangeValue(Double changeRate, Double changeValue) {
        this.changeRate = changeRate;
        this.changeValue = changeValue;
    }

    public static CalculatePeriodChangeValue of(Double changeRate, Double changeValue) {
        return CalculatePeriodChangeValue.builder()
                .changeRate(changeRate)
                .changeValue(changeValue)
                .build();
    }

    public static CalculatePeriodChangeValue ofEmpty() {
        return CalculatePeriodChangeValue.builder()
                .build();
    }
}

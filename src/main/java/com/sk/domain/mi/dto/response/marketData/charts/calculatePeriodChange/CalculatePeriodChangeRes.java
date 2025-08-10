package com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CalculatePeriodChangeRes(
        @Schema(description = "일간 변화율", example = "1.0") Double dodChangeRate,
        @Schema(description = "일간 변화 수치", example = "1.0") Double dodChangeValue,
        @Schema(description = "주간 변화율", example = "1.0") Double wowChangeRate,
        @Schema(description = "주간 변화 수치", example = "1.0") Double wowChangeValue,
        @Schema(description = "월간 변화율", example = "1.0") Double momChangeRate,
        @Schema(description = "월간 변화 수치", example = "1.0") Double momChangeValue,
        @Schema(description = "년간 변화율", example = "1.0") Double yoyChangeRate,
        @Schema(description = "년간 변화 수치", example = "1.0") Double yoyChangeValue) {

    @Builder
    public CalculatePeriodChangeRes(Double dodChangeRate, Double dodChangeValue, Double wowChangeRate, Double wowChangeValue, Double momChangeRate,
            Double momChangeValue, Double yoyChangeRate, Double yoyChangeValue) {
        this.dodChangeRate = dodChangeRate;
        this.dodChangeValue = dodChangeValue;
        this.wowChangeRate = wowChangeRate;
        this.wowChangeValue = wowChangeValue;
        this.momChangeRate = momChangeRate;
        this.momChangeValue = momChangeValue;
        this.yoyChangeRate = yoyChangeRate;
        this.yoyChangeValue = yoyChangeValue;
    }

    public static CalculatePeriodChangeRes of(Double dodChangeRate, Double dodChangeValue, Double wowChangeRate, Double wowChangeValue,
            Double momChangeRate,
            Double momChangeValue, Double yoyChangeRate, Double yoyChangeValue) {
        return CalculatePeriodChangeRes.builder()
                .dodChangeRate(dodChangeRate)
                .dodChangeValue(dodChangeValue)
                .wowChangeRate(wowChangeRate)
                .wowChangeValue(wowChangeValue)
                .momChangeRate(momChangeRate)
                .momChangeValue(momChangeValue)
                .yoyChangeRate(yoyChangeRate)
                .yoyChangeValue(yoyChangeValue)
                .build();
    }

    public static CalculatePeriodChangeRes of(CalculatePeriodChangeValue dodChange, CalculatePeriodChangeValue wowChange,
            CalculatePeriodChangeValue momChange, CalculatePeriodChangeValue yoyChange) {
        return CalculatePeriodChangeRes.builder()
                .dodChangeRate(dodChange != null ? dodChange.changeRate() : null)
                .dodChangeValue(dodChange != null ? dodChange.changeValue() : null)
                .wowChangeRate(wowChange != null ? wowChange.changeRate() : null)
                .wowChangeValue(wowChange != null ? wowChange.changeValue() : null)
                .momChangeRate(momChange != null ? momChange.changeRate() : null)
                .momChangeValue(momChange != null ? momChange.changeValue() : null)
                .yoyChangeRate(yoyChange != null ? yoyChange.changeRate() : null)
                .yoyChangeValue(yoyChange != null ? yoyChange.changeValue() : null)
                .build();
    }

    public static CalculatePeriodChangeRes ofEmpty() {
        return CalculatePeriodChangeRes.builder()
                .build();
    }
}

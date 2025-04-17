package com.sk.domain.mi.conversion.dto;

import lombok.Builder;

import java.math.BigDecimal;

public record BollingerBandResult(

    BigDecimal middle,
    BigDecimal upper,
    BigDecimal lower

) implements ConversionResult {

    @Builder
    public BollingerBandResult(BigDecimal middle, BigDecimal upper, BigDecimal lower) {
        this.middle = middle;
        this.upper = upper;
        this.lower = lower;
    }

    public static BollingerBandResult of(BigDecimal middle, BigDecimal upper, BigDecimal lower) {
        return BollingerBandResult.builder()
            .middle(middle)
            .upper(upper)
            .lower(lower)
            .build();
    }

    public static BollingerBandResult ofEmpty() {
        return BollingerBandResult.builder().build();
    }

}

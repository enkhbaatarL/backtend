package com.sk.domain.mi.conversion_on_demand.dto;

import lombok.Builder;

import java.math.BigDecimal;

public record BollingerBandResult(

    BigDecimal middle,
    BigDecimal upper,
    BigDecimal lower

) {

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

}

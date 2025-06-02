package com.sk.domain.mi.dto.response.dashboard.history;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 단일 가격 + 증감률 + 최고/최저 플래그.
 */
@Builder
public record PriceStat(
    // 가격
    BigDecimal price,
    // 직전 대비 증감률(%)
    BigDecimal changeRate,
    // 기간 최고가 여부
    boolean isMaximum,
    // 기간 최저가 여부
    boolean isMinimum
) {

    public static PriceStat of(BigDecimal price, BigDecimal changeRate, boolean isMaximum, boolean isMinimum) {
        return PriceStat.builder()
            .price(price)
            .changeRate(changeRate)
            .isMaximum(isMaximum)
            .isMinimum(isMinimum)
            .build();
    }

    public static PriceStat ifPermittedOrNull(BigDecimal price, BigDecimal changeRate, boolean isMaximum, boolean isMinimum, boolean isPermitted) {
        return isPermitted ? PriceStat.of(price, changeRate, isMaximum, isMinimum) : null;
    }

}

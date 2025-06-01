package com.sk.domain.mi.dto.response.dashboard.history;

import java.math.BigDecimal;

/**
 * 단일 가격 + 증감률 + 최고/최저 플래그.
 */
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
}

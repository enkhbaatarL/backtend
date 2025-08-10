package com.sk.domain.mi.dto.response.dashboard.history;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * 단일 가격 + 증감률 + 최고/최저 플래그.
 */
@Builder
public record PriceStat(

    @Schema(description = "가격", example = "3.5")
    BigDecimal price,

    @Schema(description = "직전 대비 증감률", example = "0.05")
    BigDecimal changeRate,

    @Schema(description = "기간 최고가 여부", example = "true")
    boolean isMaximum,

    @Schema(description = "기간 최저가 여부", example = "false")
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

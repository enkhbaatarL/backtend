package com.sk.domain.mi.dto.response.dashboard.history;

import com.sk.domain.shared.enums.PriceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PriceEntry(
    @Schema(description = "예측가 or 실제가 or 선물가", example = "FORECAST", allowableValues = {"FORECAST", "ACTUAL", "FUTURE"})
    PriceType type,
    @Schema(description = "가격 정보")
    PriceStat stat,
    @Schema(description = "권한 여부", example = "true")
    Boolean isPermitted
) {

    public static PriceEntry of(PriceType type, PriceStat stat, Boolean isPermitted) {
        return PriceEntry.builder()
            .type(type)
            .stat(stat)
            .isPermitted(isPermitted)
            .build();
    }

}

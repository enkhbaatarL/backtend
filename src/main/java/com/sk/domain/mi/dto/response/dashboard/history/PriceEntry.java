package com.sk.domain.mi.dto.response.dashboard.history;

import com.sk.domain.shared.enums.PriceType;
import lombok.Builder;

@Builder
public record PriceEntry(
    PriceType type,
    PriceStat stat,
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

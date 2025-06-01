package com.sk.domain.mi.dto.response.dashboard.history;

import com.sk.domain.shared.enums.PriceType;

public record PriceEntry(
    PriceType type,
    PriceStat stat,
    Boolean isPermitted
) {
}

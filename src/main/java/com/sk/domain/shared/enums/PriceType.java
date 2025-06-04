package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceType {

    ACTUAL("ACTUAL", "실제가"), // == SPOT(현물가)
    FUTURE("FUTURE", "선물가"),
    FORECAST("FORECAST", "예측가");

    private final String code;
    private final String description;

}

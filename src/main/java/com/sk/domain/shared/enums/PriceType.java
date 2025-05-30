package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceType {

    SPOT("SPOT", "현물가"), // ACTUAL
    FUTURES("FUTURES", "선물가"),
    FORECAST("FORECAST", "예측가");

    private final String code;
    private final String description;

}

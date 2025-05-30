package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChartPredictDataType {
    ACTUAL("ACTUAL", "실제 값"),
    FORECAST("FORECAST", "예측 값"),
    FUTURE("FUTURE", "선물 값");

    private final String code;
    private final String description;
}

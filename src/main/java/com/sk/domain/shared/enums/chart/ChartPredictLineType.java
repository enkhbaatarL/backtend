package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChartPredictLineType {
    BRANCH("BRANCH", "가지모양"),
    LINE("LINE", "선모양");

    private final String code;
    private final String description;
}

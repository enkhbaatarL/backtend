package com.sk.domain.shared.enums.dashboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DashboardChartDataTableType {
    FORECAST_SINGLE_LAYER("FORECAST_SINGLE_LAYER", 1),
    FORECAST_DUAL_LAYER("FORECAST_DUAL_LAYER", 2);

    private final String code;
    private final int layer;
}

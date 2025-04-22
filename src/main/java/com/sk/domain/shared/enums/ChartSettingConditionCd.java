package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChartSettingConditionCd {

    RANGE("range", "범위"),
    INTERVAL("currentInterval", "구간"),
    INDICATOR("indicator", "지표"),
    CHART("chart", "차트유형"),
    UNIT("currentUnitCd", "단위"),
    COMP_DATA_SOURCE("dataSource", "비교군 데이터소스");

    private final String code;
    private final String description;

}

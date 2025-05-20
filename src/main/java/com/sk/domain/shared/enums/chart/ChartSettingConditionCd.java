package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ChartSettingConditionCd {

    RANGE("range", "범위", List.of("1W", "1M", "3M", "6M", "1Y", "2Y")),
    INTERVAL("currentInterval", "구간", List.of("1D", "1W", "2W", "1M", "3M", "6M", "1Y")),
    INDICATOR("indicator", "지표", List.of("BB", "MA_5", "MA_20", "MA_60", "MA_120")),
    CHART("chart", "차트유형", List.of("LINE", "CANDLE")),
    UNIT_CD("currentUnitCd", "단위", new ArrayList<>()),
    COMP_DATA_SOURCE("dataSource", "비교군 데이터소스", new ArrayList<>());

    private final String code;
    private final String description;
    private final List<String> allowedValues;

}

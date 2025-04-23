package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Range {

    ONE_WEEK("1W"),
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1Y"),
    TWO_YEARS("2Y"),
    CUSTOM("CUSTOM");

    private final String code;

}

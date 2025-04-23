package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Interval {

    ONE_DAY("1D", "1일"),
    ONE_WEEK("1W", "1주"),
    TWO_WEEK("2W", "2주"),
    ONE_MONTH("1M", "1개월"),
    THREE_MONTH("3M", "3개월"),
    SIX_MONTH("6M", "6개월"),
    ONE_YEAR("1Y", "1년");

    private final String code;
    private final String description;

}

package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Interval {

    ONE_DAY("1DAY", "1일"),
    ONE_WEEK("1WEEK", "1주"),
    TWO_WEEK("2WEEK", "2주"),
    ONE_MONTH("1MONTH", "1개월"),
    THREE_MONTH("3MONTH", "3개월"),
    SIX_MONTH("6MONTH", "6개월"),
    ONE_YEAR("1YEAR", "1년");

    private final String code;
    private final String description;

}

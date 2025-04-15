package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Interval {

    DAY("D", "일"),
    WEEK("W", "주"),
    MONTH("M", "월"),
    YEAR("Y", "년");

    private final String code;
    private final String description;

}

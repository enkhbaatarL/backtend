package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Interval {

    DAY("DAY", "일"),
    WEEK("WEEK", "주"),
    MONTH("MONTH", "월"),
    YEAR("YEAR", "년");

    private final String code;
    private final String description;

}

package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final Map<String, Range> RANGE_CODE_MAP;

    static {
        RANGE_CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(Range::getCode, Function.identity()));
    }

    public static Range findByCode(String code) {
        return RANGE_CODE_MAP.getOrDefault(code, null);
    }

}

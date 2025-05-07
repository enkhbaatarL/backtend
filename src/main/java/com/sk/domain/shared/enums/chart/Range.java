package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Range {

    ONE_WEEK("1W", Duration.ofDays(7)),
    ONE_MONTH("1M", Duration.ofDays(30)),
    THREE_MONTHS("3M", Duration.ofDays(90)),
    SIX_MONTHS("6M", Duration.ofDays(180)),
    ONE_YEAR("1Y", Duration.ofDays(365)),
    TWO_YEARS("2Y", Duration.ofDays(730)),
    CUSTOM("CUSTOM", null);

    private final String code;
    private final Duration duration;

    private static final Map<String, Range> RANGE_CODE_MAP;

    static {
        RANGE_CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(Range::getCode, Function.identity()));
    }

    public static Range findByCode(String code) {
        return RANGE_CODE_MAP.getOrDefault(code, null);
    }

}

package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Range {

    ONE_WEEK("1W", 7),
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    TWO_YEARS("2Y", 730),
    CUSTOM("CUSTOM", Long.MAX_VALUE);

    private final String code;
    private final long approxDays;

    /**
     * interval이 허용하는 후보 중
     * 원본보다 '짧아지지 않는' 최소 Range 반환
     */
    public Range shrinkToFit(Interval interval) {
        return interval.getAllowedRanges().stream()
            .filter(r -> r.approxDays >= this.approxDays)
            .min(Comparator.comparingLong(Range::getApproxDays))
            .orElse(this);
    }


    private static final Map<String, Range> RANGE_CODE_MAP;

    static {
        RANGE_CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(Range::getCode, Function.identity()));
    }

    public static Range findByCode(String code) {
        return RANGE_CODE_MAP.getOrDefault(code, null);
    }

}

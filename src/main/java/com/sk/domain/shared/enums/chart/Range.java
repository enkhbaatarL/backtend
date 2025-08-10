package com.sk.domain.shared.enums.chart;

import com.sk.common.utils.DateUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
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

    /**
     * 주어진 {@link Range}와 시작일({@code from}), 종료일({@code to})을 기반으로 실제 날짜 문자열 쌍(시작일, 종료일)을 생성하여 반환합니다.
     * - {@code range}가 {@link #CUSTOM}인 경우:
     * - {@code from}과 {@code to} 날짜를 "yyyyMMdd" 형식의 문자열로 변환하여 반환합니다.
     * - {@code range}가 {@link #CUSTOM}이 아닌 경우 (예: {@link #ONE_WEEK}, {@link #ONE_MONTH}):
     * - {@link DateUtil#getDateRange(Range)}를 호출하여 해당 {@code Range}에 대한 날짜 문자열 쌍을 가져와 반환합니다.
     *
     * @param range 변환할 {@link Range} 객체.
     * @param from  {@code range}가 {@link #CUSTOM}일 경우 사용될 시작 날짜. 다른 {@code Range}의 경우 무시됩니다.
     * @param to    {@code range}가 {@link #CUSTOM}일 경우 사용될 종료 날짜. 다른 {@code Range}의 경우 무시됩니다.
     * @return 시작 날짜와 종료 날짜 문자열을 담은 {@link Pair}. (예: Pair.of("2023-01-01", "2023-01-07"))
     * @throws IllegalArgumentException {@code range}가 {@link #CUSTOM}이고 {@code from} 또는 {@code to}가 {@code null}인 경우.
     */
    public static Pair<String, String> toPair(Range range, LocalDate from, LocalDate to) {
        if (range == Range.CUSTOM) {
            if (from == null || to == null) {
                throw new IllegalArgumentException("To define a 'CUSTOM' date range, both a start date and an end date are required. Please provide both values.");
            }
            return Pair.of(DateUtil.toYmd(from), DateUtil.toYmd(to));
        }
        return DateUtil.getDateRange(range);
    }

}

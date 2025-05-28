package com.sk.domain.shared.enums.chart;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum Interval {

    ONE_DAY("1D", "1일", PeriodUnitSeparateCd.D, 1,
        EnumSet.of(Range.ONE_WEEK, Range.ONE_MONTH, Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    ONE_WEEK("1W", "1주", PeriodUnitSeparateCd.W, 1,
        EnumSet.of(Range.ONE_MONTH, Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    TWO_WEEK("2W", "2주", PeriodUnitSeparateCd.W, 2,
        EnumSet.of(Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    ONE_MONTH("1M", "1개월", PeriodUnitSeparateCd.M, 1,
        EnumSet.of(Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    THREE_MONTH("3M", "3개월", PeriodUnitSeparateCd.M, 3,
        EnumSet.of(Range.TWO_YEARS, Range.CUSTOM)),
    SIX_MONTH("6M", "6개월", PeriodUnitSeparateCd.M, 6,
        EnumSet.of(Range.CUSTOM)),
    ONE_YEAR("1Y", "1년", PeriodUnitSeparateCd.Y, 1,
        EnumSet.of(Range.CUSTOM));

    private final String code;
    private final String description;
    private final PeriodUnitSeparateCd periodCd;
    private final int periodCount;
    private final EnumSet<Range> allowedRanges;

    /**
     * range·데이터 최소단위·기간 길이까지 고려해
     * ① 자신이 허용되는지 판단한다
     */
    public boolean isCompatible(Range range, PeriodUnitSeparateCd minUnit, LocalDate st, LocalDate ed) {
        return this.isAllowedRange(range, st, ed) && this.periodCd.isGreaterOrEqual(minUnit);
    }

    private long toSeconds() {
        return periodCd.toSeconds(periodCount);
    }

    public boolean isAllowedRange(Range range, LocalDate startDate, LocalDate endDate) {
        if (range == Range.CUSTOM) {
            LocalDate minEndDate = startDate.plus(periodCount, periodCd.getTemporalUnit());
            return !endDate.isBefore(minEndDate);
        }
        return this.allowedRanges.contains(range);
//        if (range == Range.CUSTOM) {
//            Duration requestDuration = Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay());
//            Duration required = this.periodCd.getTemporalUnit().getDuration().multipliedBy(periodCount);
//            return requestDuration.compareTo(required) >= 0;
//        } else {
//            return this.allowedRanges.contains(range);
//        }
    }

    private static final Map<String, Interval> INTERVAL_CODE_MAP;

    static {
        INTERVAL_CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(Interval::getCode, Function.identity()));
    }

    public static Interval findByCode(String code) {
        return INTERVAL_CODE_MAP.getOrDefault(code, null);
    }

    /**
     * 같은 range 안에서 사용 가능한 가장 “짧은” Interval 찾기
     */
    public static Interval smallestAllowed(Range range, PeriodUnitSeparateCd minUnit, LocalDate st, LocalDate ed) {
        return Arrays.stream(values())
            .filter(i -> i.isCompatible(range, minUnit, st, ed))
            .min(Comparator.comparingLong(Interval::toSeconds))
            .orElse(null);
    }

}

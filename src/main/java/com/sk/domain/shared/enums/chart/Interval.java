package com.sk.domain.shared.enums.chart;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Interval {

    /**
     * 각 Interval이 적용될 수 있는 Range 목록(allowedRanges)은 이론적으로 가능한 수치도 있으나,
     * 기본적으로 설계 & 기획의 의도에 따라 명시됨
     */

    ONE_DAY("1D", "1일", PeriodUnitSeparateCd.D, 1,
        EnumSet.of(Range.ONE_WEEK, Range.ONE_MONTH, Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    ONE_WEEK("1W", "1주", PeriodUnitSeparateCd.W, 1,
        EnumSet.of(Range.ONE_MONTH, Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    TWO_WEEK("2W", "2주", PeriodUnitSeparateCd.W, 2,
        EnumSet.of(Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    ONE_MONTH("1M", "1개월", PeriodUnitSeparateCd.M, 1,
        EnumSet.of(Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    THREE_MONTH("3M", "3개월", PeriodUnitSeparateCd.Q, 3,
        EnumSet.of(Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    SIX_MONTH("6M", "6개월", PeriodUnitSeparateCd.Q, 6,
        EnumSet.of(Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM)),
    ONE_YEAR("1Y", "1년", PeriodUnitSeparateCd.Y, 1,
        EnumSet.of(Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM));

    private static final Map<String, Interval> INTERVAL_CODE_MAP;

    static {
        INTERVAL_CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(Interval::getCode, Function.identity()));
    }

    private final String code;
    private final String description;
    private final PeriodUnitSeparateCd periodCd;
    private final int periodCount;
    private final EnumSet<Range> allowedRanges;

    public static Interval findByCode(String code) {
        return INTERVAL_CODE_MAP.getOrDefault(code, null);
    }

    /**
     * 주어진 {@link Range}, 최소 기간 단위, 시작일, 종료일 조건 내에서 사용 가능한 가장 "짧은" (기간이 가장 작은) {@link Interval}을 찾습니다.
     * 모든 조건을 만족하는 Interval 중에서 기간({@link #toSeconds()})이 가장 짧은 것을 반환합니다.
     *
     * @param range     대상 {@link Range}
     * @param minUnit   허용되는 최소 기간 단위 {@link PeriodUnitSeparateCd}
     * @param startDate 기간의 시작일
     * @param endDate   기간의 종료일
     * @return 조건을 만족하는 가장 짧은 {@link Interval}. 없으면 {@code null}을 반환합니다.
     */
    public static Interval smallestAllowed(Range range, PeriodUnitSeparateCd minUnit, LocalDate startDate, LocalDate endDate) {
        return Arrays.stream(values())
            .filter(i -> i.isCompatible(range, minUnit, startDate, endDate))
            .min(Comparator.comparingLong(Interval::toSeconds))
            .orElse(null);
    }

    public static LocalDate resolveInclusiveStart(Interval interval, LocalDate startDate) {
        return switch (interval) {
            case ONE_DAY, ONE_WEEK, TWO_WEEK -> startDate;
            case ONE_MONTH, THREE_MONTH, SIX_MONTH -> startDate.withDayOfMonth(1);
            case ONE_YEAR -> startDate.withDayOfYear(1);
        };
    }

    /**
     * 현재의 Interval이 주어진 범위, 기간단위구분코드, 시작일, 종료일과 호환되는지 여부를 판단합니다.
     * 호환성 판단 기준:
     * 1. 현재 Interval이 주어진 Range에 허용되는지 ({@link #isAllowedRange(Range, LocalDate, LocalDate)}).
     * 2. 현재 Interval의 기간단위구분코드가 인자로 넘어온 기간단위구분코드보다 크거나 같은지.
     *
     * @param range                비교할 {@link Range} 객체
     * @param periodUnitSeparateCd 허용되는 최소 기간 단위 {@link PeriodUnitSeparateCd}
     * @param startDate            기간의 시작일
     * @param endDate              기간의 종료일
     * @return 호환여부
     */
    public boolean isCompatible(Range range, PeriodUnitSeparateCd periodUnitSeparateCd, LocalDate startDate, LocalDate endDate) {
        return this.isAllowedRange(range, startDate, endDate) && this.periodCd.isGreaterOrEqual(periodUnitSeparateCd);
    }

    /**
     * 현재 Interval의 기간을 초 단위로 변환하여 반환합니다.
     *
     * @return Interval의 기간(초)
     */
    private long toSeconds() {
        return periodCd.toSeconds(periodCount);
    }

    /**
     * 현재 Interval이 주어진 Range에 허용되는지 확인합니다.
     * {@link Range#CUSTOM}의 경우, 시작일과 종료일 사이의 기간이 현재 Interval의 최소 기간을 만족하는지 추가로 확인합니다.
     *
     * @param range     확인할 {@link Range}
     * @param startDate 기간의 시작일 ({@link Range#CUSTOM}인 경우 사용)
     * @param endDate   기간의 종료일 ({@link Range#CUSTOM}인 경우 사용)
     * @return 주어진 {@link Range}에 현재 Interval이 허용되면 {@code true}, 그렇지 않으면 {@code false}
     */
    public boolean isAllowedRange(Range range, LocalDate startDate, LocalDate endDate) {
        if (range == Range.CUSTOM) {
            LocalDate minEndDate = startDate.plus(periodCount, periodCd.getTemporalUnit());
            return !endDate.isBefore(minEndDate);
        }
        return this.allowedRanges.contains(range);
    }

}

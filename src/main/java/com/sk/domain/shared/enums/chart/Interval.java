package com.sk.domain.shared.enums.chart;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum Interval {

    ONE_DAY("1D", "1일", PeriodUnitSeparateCd.D, List.of(Range.ONE_WEEK, Range.ONE_MONTH, Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM), 1),
    ONE_WEEK("1W", "1주", PeriodUnitSeparateCd.W, List.of(Range.ONE_MONTH, Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM), 1),
    TWO_WEEK("2W", "2주", PeriodUnitSeparateCd.W, List.of(Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM), 2),
    ONE_MONTH("1M", "1개월", PeriodUnitSeparateCd.M, List.of(Range.THREE_MONTHS, Range.SIX_MONTHS, Range.ONE_YEAR, Range.TWO_YEARS, Range.CUSTOM), 1),
    THREE_MONTH("3M", "3개월", PeriodUnitSeparateCd.M, List.of(Range.TWO_YEARS, Range.CUSTOM), 3),
    SIX_MONTH("6M", "6개월", PeriodUnitSeparateCd.M, List.of(Range.CUSTOM), 6),
    ONE_YEAR("1Y", "1년", PeriodUnitSeparateCd.Y, List.of(Range.CUSTOM), 1);

    private final String code;
    private final String description;
    private final PeriodUnitSeparateCd periodCd;
    private final List<Range> allowedRanges;
    private final int unitCount;

    private static final Map<String, Interval> INTERVAL_CODE_MAP;

    static {
        INTERVAL_CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(Interval::getCode, Function.identity()));
    }

    public static Interval findByCode(String code) {
        return INTERVAL_CODE_MAP.getOrDefault(code, null);
    }

    public boolean isAllowedRange(Range range, LocalDate startDate, LocalDate endDate) {
        if (range == Range.CUSTOM) {
            Duration requestDuration = Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay());
            Duration required = periodCd.getTemporalUnit().getDuration().multipliedBy(unitCount);
            return requestDuration.compareTo(required) >= 0;
        } else {
            return this.allowedRanges.contains(range);
        }
    }

}

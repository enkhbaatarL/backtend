package com.sk.domain.shared.enums;

import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;

@Getter
@AllArgsConstructor
public enum PeriodUnitSeparateCd {

    D("D", "일", ChronoUnit.DAYS),
    W("W", "주", ChronoUnit.WEEKS),
    M("M", "월", ChronoUnit.MONTHS),
    Y("Y", "년", ChronoUnit.YEARS);

    private final String code;
    private final String description;
    private final TemporalUnit temporalUnit;

    public Interval minInterval() {
        return switch (this) {
            case D -> Interval.ONE_DAY;
            case W -> Interval.ONE_WEEK;
            case M -> Interval.ONE_MONTH;
            case Y -> Interval.ONE_YEAR;
        };
    }

    public Range rangeInterval() {
        return switch (this) {
            case D -> Range.SIX_MONTHS;
            case W -> Range.ONE_YEAR;
            case M -> Range.ONE_YEAR;
            case Y -> Range.TWO_YEARS;
        };
    }

    public long toSeconds(int count) {
        return temporalUnit.getDuration().multipliedBy(count).toSeconds();
    }

    /**
     * 주어진 PeriodUnitSeparateCd와 비교하여 이 PeriodUnitSeparateCd가 더 큰 단위를 가지는지 확인합니다.
     *
     * 데이터 해상도 “일 vs 주 vs 월” 같은 단위만 비교
     *
     * @param minUnit 비교 대상 PeriodUnitSeparateCd
     * @return 현재 PeriodUnitSeparateCd가 더 크다면 true, 그렇지 않으면 false
     */
    public boolean isGreaterOrEqual(PeriodUnitSeparateCd minUnit) {
        return this.temporalUnit.getDuration().compareTo(minUnit.temporalUnit.getDuration()) >= 0;
    }

}

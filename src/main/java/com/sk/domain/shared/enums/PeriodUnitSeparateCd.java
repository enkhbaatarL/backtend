package com.sk.domain.shared.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.temporal.ChronoUnit;
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

    /**
     * 주어진 PeriodUnitSeparateCd와 비교하여 이 PeriodUnitSeparateCd가 더 큰 단위를 가지는지 확인합니다.
     *
     * @param minInterval 비교 대상 PeriodUnitSeparateCd
     * @return 현재 PeriodUnitSeparateCd가 더 크다면 true, 그렇지 않으면 false
     */
    public boolean isGreaterOrEqual(PeriodUnitSeparateCd minInterval) {
        return this.temporalUnit.getDuration().compareTo(minInterval.temporalUnit.getDuration()) >= 0;
    }

}

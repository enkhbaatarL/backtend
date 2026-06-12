package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 그래프 조회 기간 단위 (월별/연도별 토글).
 * DB 적재 단위({@link PeriodUnitSeparateCd} M/Y)의 화면용 파생 enum.
 */
@Getter
@RequiredArgsConstructor
public enum GraphPeriodUnit {

    MONTHLY("monthly", "월별", PeriodUnitSeparateCd.M),
    YEARLY("yearly", "연도별", PeriodUnitSeparateCd.Y);

    @JsonValue
    private final String code;
    private final String label;
    private final PeriodUnitSeparateCd dbUnit;

    @JsonCreator
    public static GraphPeriodUnit from(String value) {
        return Arrays.stream(values())
            .filter(unit -> unit.name().equalsIgnoreCase(value) || unit.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown GraphPeriodUnit: %s".formatted(value)));
    }

    /** 기간키(YYYYMM / YYYY)를 응답 날짜 표기(2026-06 / 2026)로 변환한다. */
    public String formatPeriodKey(String periodKey) {
        return this == MONTHLY
            ? "%s-%s".formatted(periodKey.substring(0, 4), periodKey.substring(4))
            : periodKey;
    }

}

package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 글로벌 LNG 가격 압력 구간 (tmm_mi_scenario_m.global_pressure_band, v4 전용).
 */
@Getter
@RequiredArgsConstructor
public enum GlobalPressureBand {

    STRONG_BULLISH("strong_bullish", "강한 상승"),
    BULLISH("bullish", "상승"),
    BALANCED("balanced", "중립"),
    BEARISH("bearish", "하락"),
    STRONG_BEARISH("strong_bearish", "강한 하락");

    private final String code;
    private final String label;

    /** Agent 응답 문자열 파싱. 미지 값은 null (Agent 어휘 확장 내성). */
    public static GlobalPressureBand fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(band -> band.name().equalsIgnoreCase(value) || band.code.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }

}

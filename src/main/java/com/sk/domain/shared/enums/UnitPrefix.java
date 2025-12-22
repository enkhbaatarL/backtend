package com.sk.domain.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 경제 지표 단위 접두사 Enum
 *
 * <p>Refinitiv 경제 달력 데이터에서 사용되는 단위 접두사를 정의합니다.</p>
 *
 * @see com.sk.domain.shared.entity.master.tif.EconomicCalendar #unitPrefix
 */
@Getter
@RequiredArgsConstructor
public enum UnitPrefix {

    BILLION("Bln", "B", "10억", 1_000_000_000L),
    MILLION("Mln", "M", "100만", 1_000_000L),
    TRILLION("Trl", "T", "1조", 1_000_000_000_000L),
    THOUSAND("Thou", "K", "1천", 1_000L);

    private final String code;

    @JsonValue
    private final String displayValue;

    private final String description;

    private final Long multiplier;

    /**
     * 코드로 UnitPrefix 찾기
     *
     * @param code 단위 접두사 코드 (예: "Bln", "Mln")
     * @return 해당하는 UnitPrefix, 없으면 null
     */
    public static UnitPrefix fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        for (UnitPrefix prefix : values()) {
            if (prefix.code.equalsIgnoreCase(code)) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * 표시 값으로 UnitPrefix 찾기
     *
     * @param displayValue 표시 값 (예: "B", "M")
     * @return 해당하는 UnitPrefix, 없으면 null
     */
    public static UnitPrefix fromDisplayValue(String displayValue) {
        if (displayValue == null || displayValue.isBlank()) {
            return null;
        }

        for (UnitPrefix prefix : values()) {
            if (prefix.displayValue.equalsIgnoreCase(displayValue)) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * 원본 값에 배율을 적용
     *
     * @param value 원본 값
     * @return 배율이 적용된 값
     */
    public BigDecimal applyMultiplier(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.multiply(BigDecimal.valueOf(this.multiplier));
    }
}

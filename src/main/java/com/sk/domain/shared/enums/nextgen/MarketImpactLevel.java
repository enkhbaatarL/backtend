package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketImpactLevel {

    LOW("low", "낮음"),
    MEDIUM("medium", "중간"),
    HIGH("high", "높음");

    @JsonValue
    private final String code;
    private final String label;

    public static MarketImpactLevel fromCode(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        String lower = dbValue.toLowerCase();
        for (MarketImpactLevel level : values()) {
            if (level.code.equals(lower)) {
                return level;
            }
        }
        return null;
    }

}

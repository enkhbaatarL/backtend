package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketDirection {

    BULLISH("bullish", "상승"),
    BEARISH("bearish", "하락"),
    NEGLIGIBLE("negligible", "보합");

    @JsonValue
    private final String code;
    private final String label;

    public static MarketDirection fromCode(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        String lower = dbValue.toLowerCase();
        for (MarketDirection direction : values()) {
            if (direction.code.equals(lower)) {
                return direction;
            }
        }
        return null;
    }

}

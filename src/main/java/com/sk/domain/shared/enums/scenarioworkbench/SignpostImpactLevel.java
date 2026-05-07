package com.sk.domain.shared.enums.scenarioworkbench;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SignpostImpactLevel {

    STRONG("strong", "강함"),
    MODERATE("moderate", "보통"),
    WEAK("weak", "약함");

    @JsonValue
    private final String code;
    private final String label;

    public static SignpostImpactLevel fromCode(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        String lower = dbValue.toLowerCase();
        for (SignpostImpactLevel level : values()) {
            if (level.code.equals(lower)) {
                return level;
            }
        }
        return null;
    }

}

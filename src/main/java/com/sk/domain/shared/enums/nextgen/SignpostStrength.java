package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Signpost 영향도 (시나리오 확률에 주는 강도). DB 적재 코드는 대문자(STRONG/MEDIUM/WEAK).
 */
@Getter
@RequiredArgsConstructor
public enum SignpostStrength {

    STRONG("strong", "강함"),
    MEDIUM("medium", "보통"),
    WEAK("weak", "약함");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static SignpostStrength from(String value) {
        return Arrays.stream(values())
            .filter(strength -> strength.name().equalsIgnoreCase(value) || strength.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown SignpostStrength: " + value));
    }

}

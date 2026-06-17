package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Signpost 영향도 (시나리오 확률에 주는 강도).
 * <p>
 * DB 적재 코드는 대문자(VERYSTRONG/STRONG/MEDIUM/WEAK/VERYWEAK).
 * Agent 기획이 초기 3단계에서 5단계로 변경됨에 따라 5단계로 운영한다.
 */
@Getter
@RequiredArgsConstructor
public enum SignpostStrength {

    VERYSTRONG("verystrong", "매우높음"),
    STRONG("strong", "높음"),
    MEDIUM("medium", "보통"),
    WEAK("weak", "낮음"),
    VERYWEAK("veryweak", "매우낮음");

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

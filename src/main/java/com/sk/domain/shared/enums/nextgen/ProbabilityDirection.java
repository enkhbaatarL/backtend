package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Signpost가 시나리오 확률에 주는 변화 방향.
 */
@Getter
@RequiredArgsConstructor
public enum ProbabilityDirection {

    SUP("sup", "가능성강화");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static ProbabilityDirection from(String value) {
        return Arrays.stream(values())
            .filter(direction -> direction.name().equalsIgnoreCase(value) || direction.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ProbabilityDirection: " + value));
    }

}

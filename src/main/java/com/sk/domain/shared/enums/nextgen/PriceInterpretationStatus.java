package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오별 가격전망 해석 상태
 */
@Getter
@RequiredArgsConstructor
public enum PriceInterpretationStatus {

    GENERATING("generating", "생성중"),
    GENERATED("generated", "생성완료"),
    FAILED("failed", "생성실패");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static PriceInterpretationStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown PriceInterpretationStatus: %s".formatted(value)));
    }

    public boolean isGenerating() {
        return this == GENERATING;
    }

}

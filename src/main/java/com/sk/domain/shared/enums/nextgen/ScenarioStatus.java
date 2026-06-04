package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 판정 상태 (가정 및 실행 가능성 표의 '상태'). 대기 → 실현/폐기.
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioStatus {

    PENDING("pending", "대기"),
    REALIZED("realized", "실현"),
    DISCARDED("discarded", "폐기");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static ScenarioStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioStatus: " + value));
    }

}

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

    /**
     * DB judgment_status_code(한글 라벨)를 enum으로 역매핑.
     * 컬럼은 NULL 허용이며 기본값이 '대기'이므로 null/blank 는 PENDING 으로 처리한다.
     */
    public static ScenarioStatus fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return PENDING;
        }
        return Arrays.stream(values())
            .filter(status -> status.label.equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioStatus label: " + label));
    }

}

package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ModelExecutionStatus {

    RUNNING("RUNNING", "실행 중"),
    COMPLETED("COMPLETED", "완료"),
    FAILED("FAILED", "실패");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static ModelExecutionStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ModelExecutionStatus: " + value));
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}

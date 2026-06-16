package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Agent 콜백의 작업 성공 여부
 */
@Getter
@RequiredArgsConstructor
public enum AgentCallbackResult {

    SUCCESS("success"),
    FAILURE("failure");

    @JsonValue
    private final String code;

    @JsonCreator
    public static AgentCallbackResult from(String value) {
        return Arrays.stream(values())
            .filter(result -> result.name().equalsIgnoreCase(value) || result.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown AgentCallbackResult: " + value));
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

}

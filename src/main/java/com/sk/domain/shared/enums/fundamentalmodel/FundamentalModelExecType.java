package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelExecType {

    INDEPENDENT("INDEPENDENT", "독립실행"),
    SCENARIO("SCENARIO", "시나리오실행"),
    MI_BASE("MI_BASE", "MI Base");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static FundamentalModelExecType from(String value) {
        return Arrays.stream(values())
            .filter(type -> type.name().equalsIgnoreCase(value) || type.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelExecType: " + value));
    }
}

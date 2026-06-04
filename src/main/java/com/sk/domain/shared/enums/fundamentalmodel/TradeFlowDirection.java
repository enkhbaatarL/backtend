package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeFlowDirection {

    TO_COUNTRY("TO_COUNTRY"),
    FROM_COUNTRY("FROM_COUNTRY");

    @JsonValue
    private final String code;

    @JsonCreator
    public static TradeFlowDirection from(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(d -> d.name().equalsIgnoreCase(value) || d.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown TradeFlowDirection: " + value));
    }
}

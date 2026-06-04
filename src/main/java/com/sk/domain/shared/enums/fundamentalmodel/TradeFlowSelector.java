package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeFlowSelector {

    LNG("LNG"),
    PNG("PNG");

    @JsonValue
    private final String code;

    @JsonCreator
    public static TradeFlowSelector from(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(s -> s.name().equalsIgnoreCase(value) || s.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown TradeFlowSelector: " + value));
    }
}

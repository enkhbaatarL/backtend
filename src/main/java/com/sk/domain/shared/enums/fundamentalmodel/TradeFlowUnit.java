package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeFlowUnit {

    MTPA("MTPA"),
    BCM("BCM");

    @JsonValue
    private final String code;

    @JsonCreator
    public static TradeFlowUnit from(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(u -> u.name().equalsIgnoreCase(value) || u.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown TradeFlowUnit: " + value));
    }
}

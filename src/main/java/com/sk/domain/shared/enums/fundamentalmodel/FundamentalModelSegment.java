package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelSegment {

    GLOBAL("GLOBAL", "Global"),
    LNG("LNG", "LNG"),
    TRADE("TRADE", "Trade");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static FundamentalModelSegment from(String value) {
        return Arrays.stream(values())
            .filter(segment -> segment.name().equalsIgnoreCase(value) || segment.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelSegment: " + value));
    }
}

package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelGranularity {

    MONTHLY("MONTHLY", "Monthly"),
    YEARLY("YEARLY", "Yearly");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static FundamentalModelGranularity from(String value) {
        return Arrays.stream(values())
            .filter(granularity -> granularity.name().equalsIgnoreCase(value) || granularity.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelGranularity: " + value));
    }
}

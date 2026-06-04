package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeCountryHeaderKind {

    REGION_TOTAL("REGION_TOTAL", "Region total"),
    COUNTRY("COUNTRY", "Country"),
    GRAND_TOTAL("GRAND_TOTAL", "Grand total");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static TradeCountryHeaderKind from(String value) {
        return Arrays.stream(values())
            .filter(kind -> kind.name().equalsIgnoreCase(value) || kind.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown TradeCountryHeaderKind: " + value));
    }
}

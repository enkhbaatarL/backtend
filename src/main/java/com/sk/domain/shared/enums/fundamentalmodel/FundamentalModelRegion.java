package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelRegion {

    ASIA("ASIA", "Asia"),
    EUROPE("EUROPE", "Europe"),
    NORTH_AMERICA("NORTH_AMERICA", "North America"),
    MIDDLE_EAST("MIDDLE_EAST", "Middle East"),
    AUSTRALIA("AUSTRALIA", "Australia"),
    RUSSIA("RUSSIA", "Russia"),
    AFRICA("AFRICA", "Africa"),
    SOUTH_AMERICA("SOUTH_AMERICA", "South America");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static FundamentalModelRegion from(String value) {
        return Arrays.stream(values())
            .filter(region -> region.name().equalsIgnoreCase(value) || region.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelRegion: " + value));
    }
}

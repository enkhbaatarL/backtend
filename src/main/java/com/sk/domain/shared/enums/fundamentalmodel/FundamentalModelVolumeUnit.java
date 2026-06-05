package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelVolumeUnit {

    MTPA("MTPA");

    @JsonValue
    private final String code;

    @JsonCreator
    public static FundamentalModelVolumeUnit from(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(unit -> unit.name().equalsIgnoreCase(value) || unit.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelVolumeUnit: " + value));
    }
}

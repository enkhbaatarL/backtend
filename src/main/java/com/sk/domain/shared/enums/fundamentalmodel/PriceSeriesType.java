package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceSeriesType {

    BASE_CASE("BASE_CASE", "Base case price"),
    USER_ADJUSTED("USER_ADJUSTED", "User adjusted price");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static PriceSeriesType from(String value) {
        return Arrays.stream(values())
            .filter(seriesType -> seriesType.name().equalsIgnoreCase(value) || seriesType.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown PriceSeriesType: " + value));
    }
}

package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VolumesRowKind {

    COUNTRY("COUNTRY", "Country"),
    REGION_SUBTOTAL("REGION_SUBTOTAL", "Region subtotal"),
    TYPE_TOTAL("TYPE_TOTAL", "Type total");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static VolumesRowKind from(String value) {
        return Arrays.stream(values())
            .filter(rowKind -> rowKind.name().equalsIgnoreCase(value) || rowKind.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown VolumesRowKind: " + value));
    }
}

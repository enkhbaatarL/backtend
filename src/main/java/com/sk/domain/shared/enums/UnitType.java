package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UnitType {

    TON("TON", "톤"),
    BARREL("BARREL", "배럴"),
    METER("METER", "미터"),
    HPA("HPA", "헥토파스칼");

    private final String code;
    private final String description;

    public static UnitType fromCode(String code) {
        return Arrays.stream(values())
            .filter(unitType -> unitType.getCode().equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown unit code: " + code));
    }

    public static UnitType getUnitTypeByName(String name) {
        return Arrays.stream(UnitType.values())
            .filter(unitType -> unitType.getCode().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown unit name: " + name));
    }

    public static String generateConversionKey(UnitType fromUnit, UnitType toUnit) {
        return fromUnit.name() + "_TO_" + toUnit.name();
    }

}

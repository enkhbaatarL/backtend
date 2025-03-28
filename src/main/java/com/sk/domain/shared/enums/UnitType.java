package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UnitType {

    BARREL("barrel", "배럴"),
    TON("ton", "톤"),
    GALLON("gallon", "갤런"),
    LITER("liter", "리터"),
    INCH("inch", "인치"),
    CM("cm", "센티미터"),
    WAFER("wafer", "웨이퍼"),
    EA("ea", "개");

    private final String code;
    private final String description;

    public static UnitType getUnitTypeByCode(String code) {
        return Arrays.stream(UnitType.values())
            .filter(unitType -> unitType.getCode().equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown unit name: " + code));
    }

}

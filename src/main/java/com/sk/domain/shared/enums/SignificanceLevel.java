package com.sk.domain.shared.enums;

import java.util.Arrays;
import lombok.Getter;

/**
 * 중요도 레벨
 */
@Getter
public enum SignificanceLevel {
    HIGH((byte) 3, "High"),
    MEDIUM((byte) 2, "Medium"),
    LOW((byte) 1, "Low");

    private final Byte code;
    private final String name;

    SignificanceLevel(Byte code, String name) {
        this.code = code;
        this.name = name;
    }

    public static SignificanceLevel fromCode(Byte code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(level -> level.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public static String getNameFromCode(Byte code) {
        SignificanceLevel level = fromCode(code);
        return level != null ? level.getName() : null;
    }
}

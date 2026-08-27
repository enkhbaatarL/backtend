package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DesignOperationType {

    CREATE("create", "생성"),
    VALIDATION("validation", "검토"),
    SIGNPOST("signpost", "Signpost 생성"),
    CONFIRMATION("confirmation", "확정");

    private final String code;
    private final String label;

    /** Agent 응답 문자열 파싱. 미지 값은 null (Agent 어휘 확장 내성). */
    public static DesignOperationType fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(type -> type.name().equalsIgnoreCase(value) || type.code.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }

}

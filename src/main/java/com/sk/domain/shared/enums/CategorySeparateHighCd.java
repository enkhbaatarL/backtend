package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum CategorySeparateHighCd {

    LANGUAGES("A_LANGUAGES", "언어"),
    TYPE("A_TYPES", "타입"),
    PROVIDERS("A_PROVIDERS", "프로바이더"),
    NONE("NONE", "매칭되지 않음");

    private final String codeId;
    private final String description;

    private static final Map<String, CategorySeparateHighCd> HIGH_CD_MAP;

    static {
        HIGH_CD_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(cd -> cd.codeId, Function.identity()));
    }

    public static CategorySeparateHighCd fromCodeId(String codeId) {
        return HIGH_CD_MAP.getOrDefault(codeId, NONE);
    }

}

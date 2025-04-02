package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotiCrtvlSptCd {
    UPPER("UPPER", "상한"),
    LOWER("LOWER", "하한");

    private final String code;
    private final String description;
}

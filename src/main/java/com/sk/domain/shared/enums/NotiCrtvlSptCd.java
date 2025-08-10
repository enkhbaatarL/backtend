package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotiCrtvlSptCd {
    
    UPPER("UPPER", "상승"),
    LOWER("LOWER", "하락"),
    INCRATE("INCRATE", "상승률"),
    DECRATE("DECRATE", "하락률");

    private final String code;
    private final String description;
}

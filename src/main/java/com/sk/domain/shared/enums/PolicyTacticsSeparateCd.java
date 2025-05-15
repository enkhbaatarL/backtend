package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PolicyTacticsSeparateCd {

    PRIVACY_POLICY("01", "개인정보취급방침"),
    TERMS_OF_SERVICE("02", "서비스이용약관");

    private final String code;
    private final String description;

    public static PolicyTacticsSeparateCd of(String code) {
        for (PolicyTacticsSeparateCd value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

}

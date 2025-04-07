package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRoleCd {

    USER("USER", "사용자"),
    ADMIN("ADMIN", "관리자"),
    OPERATOR("OPERATOR", "운영자");

    private final String code;
    private final String description;

}

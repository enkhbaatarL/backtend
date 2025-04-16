package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    USER("USER", "사용자"),
    ADMIN("ADMIN", "관리자"),
    SUB_ADMIN("SUB_ADMIN", "관리자(public 상품 업로드)"),
    OPERATOR("OPERATOR", "운영자(private 상품 업로드)"),
    CLIENT("CLIENT", "클라이언트");

    private final String code;
    private final String description;

}

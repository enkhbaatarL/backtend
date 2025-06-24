package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    /**
     * SSO 로그인 이후 발급되는 '사용자' 토큰
     */
    USER("USER", "사용자"),

    /**
     * SSO 로그인 이후 발급되는 '관리자' 토큰
     */
    ADMIN("ADMIN", "관리자(Private 상품 업로더)"),

    /**
     * SSO 로그인 이후 발급되는 '운영자' 토큰
     *   - 사용자가 직접 RPA 상품(Private) 업로드를 위한 토큰
     */
    OPERATOR("OPERATOR", "운영자"),

    /**
     * Oauth2 인증 이후 발급되는 '회사 단위 관리자' 토큰
     *   - API를 통하여 RPA 상품(Public) 업로드를 위한 토큰
     */
    API_SUB_ADMIN("API_SUB_ADMIN", "관리자(Public 상품 업로더)"),

    /**
     * Oauth2 인증 이후 발급되는 '회사 단위 운영자' 토큰
     *   - API를 통하여 RPA 상품(Private) 업로드를 위한 토큰
     *   - 자신의 회사가 '회사권한'을 가졌다면 해당하는 상품 리소스에 접근이 가능
     */
    API_PLUS_OPERATOR("API_PLUS_OPERATOR", "운영자(Private 상품 업로드, 자사 권한 상품 리소스 접근)"),

    /**
     * 기본 Default Role / 실제 발급되는 claim은 아님.
     */
    SYSTEM("SYSTEM", "시스템");

    private final String code;
    private final String description;

}

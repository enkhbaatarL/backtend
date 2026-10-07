package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이벤트 등록 출처.
 * 사용자 이벤트는 Agent가 MCP 요청으로 적재하며 생성자/수정자 로그인 ID를 `mcp_{userLoginId}`로 남긴다 (파이프라인은 SYSTEM).
 * 생성자·수정자 중 하나라도 사용자면 사용자 등록으로 본다.
 */
@Getter
@RequiredArgsConstructor
public enum EventSourceType {

    PLATFORM("platform", "플랫폼 등록"),
    USER("user", "사용자 등록");

    /** 사용자 등록 이벤트의 생성자/수정자 로그인 ID 접두어 */
    public static final String USER_LOGIN_ID_PREFIX = "mcp_";

    @JsonValue
    private final String code;
    private final String label;

    public static EventSourceType of(String createLoginUserId, String modifyLoginUserId) {
        return isUserLoginId(createLoginUserId) || isUserLoginId(modifyLoginUserId) ? USER : PLATFORM;
    }

    /** 사용자 등록 이벤트의 등록자 로그인 ID — 생성자 우선, 없으면 수정자. 판별 불가면 null */
    public static String registrantLoginId(String createLoginUserId, String modifyLoginUserId) {
        String loginId = stripPrefix(createLoginUserId);
        return loginId != null ? loginId : stripPrefix(modifyLoginUserId);
    }

    /** 사용자 로그인 ID의 이벤트 등록 ID (`mcp_{userLoginId}`) */
    public static String registeredLoginId(String userLoginId) {
        return USER_LOGIN_ID_PREFIX + userLoginId;
    }

    private static boolean isUserLoginId(String loginId) {
        return loginId != null && loginId.startsWith(USER_LOGIN_ID_PREFIX);
    }

    private static String stripPrefix(String loginId) {
        if (!isUserLoginId(loginId)) return null;
        String userLoginId = loginId.substring(USER_LOGIN_ID_PREFIX.length());
        return userLoginId.isBlank() ? null : userLoginId;
    }

}

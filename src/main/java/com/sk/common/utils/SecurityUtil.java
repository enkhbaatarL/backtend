package com.sk.common.utils;

import com.sk.domain.shared.enums.Claim;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * 현재 인증 컨텍스트(JWT)에서 로그인 사용자 정보를 읽는 유틸.
 * <p>{@link com.sk.common.resolver.UserArgumentResolver}와 동일하게 JWT 클레임 {@code USER_NO}를 본다.
 * 다만 비인증/미식별이면 예외 대신 빈 값을 반환해, 점유자 표기 등 부가 정보에 부담 없이 쓰도록 한다.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** 현재 로그인 사용자의 user_no. 비인증이거나 클레임이 없으면 empty. */
    public static Optional<Long> currentUserNo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            return Optional.empty();
        }
        String userNo = jwtToken.getToken().getClaimAsString(Claim.USER_NO.getCode());
        if (userNo == null || userNo.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(userNo.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

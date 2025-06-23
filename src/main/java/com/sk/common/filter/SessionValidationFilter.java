package com.sk.common.filter;

import com.sk.common.config.security.jwt.CustomJwtDecoder;
import com.sk.common.enums.AuthComponent;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthenticationException;
import com.sk.domain.shared.enums.Claim;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionValidationFilter extends OncePerRequestFilter {

    private final CustomJwtDecoder jwtDecoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {

            try {

                Jwt jwt = jwtDecoder.decode(token); // 토큰 디코딩
                String authMethod = jwt.getClaimAsString(Claim.AUTH_METHOD.getCode());

                // 1. 'auth_method' 클레임이 있고, 그 값이 'saml2' 인 경우에만 세션 검사를 수행
                if (Claim.AuthMethodValue.SAML2.getCode().equals(authMethod)) {

                    // 세션이 없으면 null을 반환 (새로 생성하지 않음)
                    HttpSession session = request.getSession(false);

                    // 2. 세션이 존재하지 않으면, 유휴 시간 초과로 간주하고 401 Unauthorized 응답
                    if (session == null) {
                        log.info("SAML SSO user's session has expired. Denying access for token: {}", jwt.getId());
                        throw CustomAuthenticationException.of(AuthComponent.OAUTH2_RESOURCE, ResultCode.Error.AUTH_SESSION_INVALID);
                    }

                }

            } catch (JwtException e) {
                // 토큰 디코딩 자체에 실패한 경우 (만료, 서명 불일치 등)
                // 이 필터에서는 아무것도 하지 않고 다음 필터(BearerTokenAuthenticationFilter)가 처리하도록 넘김
                log.debug("Invalid JWT, letting subsequent filters handle it. Error: {}", e.getMessage());
            }

        }

        // 그 외 모든 경우 (토큰이 없거나, sso 토큰이 아니거나, 세션이 유효한 경우)는 필터 체인을 계속 진행
        filterChain.doFilter(request, response);

    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}

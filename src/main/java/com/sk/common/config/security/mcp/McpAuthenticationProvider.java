package com.sk.common.config.security.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * [MCP 인증] MCP API 의 Basic Auth 검증. tmd_mcpauthinfom 만 조회하며 OAuth 클라이언트(tif_clntaccsinfom)와 무관합니다.
 * 빈으로 등록하지 않습니다. 빈이면 전역 AuthenticationManager 에 섞여 다른 체인에서도 쓰일 수 있으므로
 * ResourceServerConfig 의 MCP 체인에서만 생성해 사용합니다.
 */
@RequiredArgsConstructor
public class McpAuthenticationProvider implements AuthenticationProvider {

    private final McpCredentialVerifier mcpCredentialVerifier;

    @Override
    public Authentication authenticate(Authentication authentication) {
        String secret = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
        return mcpCredentialVerifier.verify(authentication.getName(), secret);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

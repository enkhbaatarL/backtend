package com.sk.common.config.security.mcp;

import java.util.List;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * [MCP 인증] Basic Auth 로 인증된 MCP 클라이언트(시스템). 사용자가 아니라 회사 단위로 묶입니다.
 * McpUserContextInterceptor 가 X-User-Id 사용자의 회사와 비교한 뒤 사용자 컨텍스트로 교체합니다.
 */
@Getter
public class McpClientAuthenticationToken extends AbstractAuthenticationToken {

    private final String mcpAuthId;
    private final Integer customerCompanyNo;
    private final boolean managementCompany;

    public McpClientAuthenticationToken(String mcpAuthId, Integer customerCompanyNo, boolean managementCompany) {
        super(List.of());
        this.mcpAuthId = mcpAuthId;
        this.customerCompanyNo = customerCompanyNo;
        this.managementCompany = managementCompany;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return mcpAuthId;
    }

}

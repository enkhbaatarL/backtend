package com.sk.domain.admin.dto.response.mcp;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * [MCP 인증] 발급·재발급 응답. 평문 시크릿은 이 응답에서 한 번만 내려가고 서버에는 해시만 남습니다.
 */
public record McpAuthIssueRes(

    @Schema(description = "MCP 인증 정보")
    McpAuthInfoRes mcpAuthInfo,

    @Schema(description = "평문 시크릿 (발급 시 1회만 제공, 다시 조회할 수 없습니다)", example = "q8Jm2...43자")
    String secret

) {

    public static McpAuthIssueRes of(McpAuthInfoRes mcpAuthInfo, String secret) {
        return new McpAuthIssueRes(mcpAuthInfo, secret);
    }

}

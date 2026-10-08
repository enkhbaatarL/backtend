package com.sk.domain.admin.dto.response.mcp;

import io.swagger.v3.oas.annotations.media.Schema;

/** [MCP 인증] MCP 인증 ID 중복 확인 응답 (화면의 입력 중 중복 확인용). */
public record McpAuthExistsRes(

    @Schema(description = "이미 사용 중인 ID 인지 여부", example = "false")
    Boolean exists

) {

    public static McpAuthExistsRes of(boolean exists) {
        return new McpAuthExistsRes(exists);
    }

}

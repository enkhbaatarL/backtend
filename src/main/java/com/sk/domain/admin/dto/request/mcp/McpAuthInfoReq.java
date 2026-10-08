package com.sk.domain.admin.dto.request.mcp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** [MCP 인증] MCP 인증 키 등록 요청. ID 규칙은 frontend 입력 검사와 같다. */
public class McpAuthInfoReq {

    /** 입력 ID 규칙. '_' 를 허용하지 않아 삭제 시 부여하는 d_ 형식과 겹치지 않습니다. */
    public static final String MCP_AUTH_ID_REGEXP = "^[A-Za-z0-9-]+$";
    public static final int MCP_AUTH_ID_MAX_LENGTH = 50;

    @Schema(name = "McpAuthInfoReq.Insert")
    public record Insert(

        @Schema(description = "고객회사 번호", example = "1000005")
        @NotNull
        Integer customerCompanyNo,

        @Schema(
            description = "MCP 인증 ID (Basic Auth username)",
            example = "skcc-mcp01",
            pattern = MCP_AUTH_ID_REGEXP,
            maxLength = MCP_AUTH_ID_MAX_LENGTH
        )
        @NotBlank
        @Size(max = MCP_AUTH_ID_MAX_LENGTH)
        @Pattern(
            regexp = MCP_AUTH_ID_REGEXP,
            message = "Only English letters, digits and hyphens are allowed."
        )
        String mcpAuthId

    ) {

    }

}

package com.sk.domain.shared.dto.response.external;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "외부 로그인 응답")
public record ExternalLoginRes(

    @Schema(description = "세션 ID (콜백 API 호출 시 사용)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    String sessionId

) {
    public static ExternalLoginRes of(String sessionId) {
        return new ExternalLoginRes(sessionId);
    }
}
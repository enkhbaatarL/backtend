package com.sk.domain.mi.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;

public record StreamCancelReq(
    @Schema(description = "스트림 중단 내역 또는 전송받은 답변 일부", example = "## 2025-12-01 기준 Brent 원유 프론트먼스 정산가 분")
    String message
) {
}

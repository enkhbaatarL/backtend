package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ModelParamProposalRes(

    @Schema(description = "Agent model parameter proposal ID", example = "10001")
    Long modelParamProposalId,

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "시나리오 그룹 ID", example = "61")
    Long scenarioGroupId,

    @Schema(description = "MI Event ID", example = "1378")
    Long eventId,

    @Schema(description = "FE 표시 상태. Agent status 또는 Backend overlay status", example = "PROPOSED")
    String status,

    @Schema(description = "flat model parameter array")
    JsonNode parameters,

    @Schema(description = "실패 사유")
    String failureReason,

    @Schema(description = "Backend proposal 요청 접수 시각")
    LocalDateTime requestedAt,

    @Schema(description = "Agent proposal 완료 또는 confirmation 시각")
    LocalDateTime completedAt
) {
}

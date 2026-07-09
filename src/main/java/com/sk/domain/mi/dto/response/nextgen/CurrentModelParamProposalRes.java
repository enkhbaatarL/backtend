package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.enums.nextgen.CurrentModelParamProposalState;
import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import com.sk.domain.shared.enums.nextgen.ModelParamParameterSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CurrentModelParamProposalRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "Scenario 기준 팝업 표시 상태", implementation = CurrentModelParamProposalState.class, example = "PROPOSAL_READY")
    CurrentModelParamProposalState currentState,

    @Schema(description = "반환 파라미터 출처", implementation = ModelParamParameterSource.class, example = "PROPOSAL_PARAMETERS")
    ModelParamParameterSource parameterSource,

    @Schema(description = "Backend execution-preparation ID", example = "51001")
    Long execPrepId,

    @Schema(description = "Agent model parameter proposal ID", example = "10001")
    Long modelParamProposalId,

    @Schema(description = "시나리오 그룹 ID", example = "61")
    Long scenarioGroupId,

    @Schema(description = "MI Event ID", example = "1378")
    Long eventId,

    @Schema(description = "연결 Scenario Case ID", example = "88001")
    Long caseId,

    @Schema(description = "연결 Case의 latest run ID", example = "20260709_153000_abcd")
    String runId,

    @Schema(description = "기존 수급모델 실행 상태", implementation = ModelExecutionStatus.class, example = "COMPLETED")
    ModelExecutionStatus modelExecutionStatus,

    @Schema(description = "flat model parameter array")
    JsonNode parameters,

    @Schema(description = "실패 사유")
    String failureReason,

    @Schema(description = "Backend proposal 요청 접수 시각")
    LocalDateTime requestedAt,

    @Schema(description = "Agent proposal 완료 시각")
    LocalDateTime completedAt,

    @Schema(description = "Agent confirmation 완료 시각")
    LocalDateTime confirmedAt
) {
}

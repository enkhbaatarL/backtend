package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.CurrentModelParamProposalState;
import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import com.sk.domain.shared.enums.nextgen.ModelParamParameterSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record CurrentModelParamProposalRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "Scenario 기준 팝업 표시 상태. 새 명시적 생성 오류는 PROPOSAL_FAILED이며 PROPOSAL_NO_CHANGES는 과거 데이터 읽기 호환용 실패 상태입니다.", implementation = CurrentModelParamProposalState.class, example = "PROPOSAL_READY")
    CurrentModelParamProposalState currentState,

    @Schema(description = "반환 파라미터 출처", implementation = ModelParamParameterSource.class, example = "PROPOSAL_PARAMETERS")
    ModelParamParameterSource parameterSource,

    @Schema(description = "Backend 요청 이력 ID. 요청 전에는 null", example = "51001", nullable = true)
    Long execPrepId,

    @Schema(description = "Agent model parameter proposal ID. ID 발급 전 생성 실패이면 null일 수 있습니다.", example = "10001", nullable = true)
    Long modelParamProposalId,

    @Schema(description = "시나리오 그룹 ID. Agent proposal이 없으면 null", example = "61", nullable = true)
    Long scenarioGroupId,

    @Schema(description = "연결 Scenario Case ID. 실행 연결 전에는 null", example = "88001", nullable = true)
    Long caseId,

    @Schema(description = "연결 Case의 latest run ID. 실행 연결 전에는 null", example = "20260709_153000_abcd", nullable = true)
    String runId,

    @Schema(description = "기존 수급모델 실행 상태. 실행 연결 전에는 null", implementation = ModelExecutionStatus.class, example = "COMPLETED", nullable = true)
    ModelExecutionStatus modelExecutionStatus,

    @Schema(description = "성공한 실행의 DEMAND/SUPPLY/TRADE/OUTPUT input 파일 projection. EXECUTED_CONFIRMED 외 상태에서는 null", nullable = true)
    ModelExecutionInputFilesRes executionInputFiles,

    @Schema(
        description = "A4a 전용 modelFile > modelSheet > modelTable > parameter/rendering tree. parameterSource=NONE이면 빈 배열",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<CurrentModelParamFileRes> modelFiles,

    @Schema(description = "실패 사유. 새 생성 오류에는 정규화한 사용자용 유형 문구를 반환하며, 실패 사유가 없으면 null", nullable = true)
    String failureReason,

    @Schema(description = "Backend proposal 요청 접수 시각. 요청 전에는 null", nullable = true)
    LocalDateTime requestedAt,

    @Schema(description = "Agent proposal 완료 시각. Agent proposal 또는 완료 시각이 없으면 null", nullable = true)
    LocalDateTime completedAt,

    @Schema(description = "Agent confirmation 완료 시각. 확정 전에는 null", nullable = true)
    LocalDateTime confirmedAt
) {
}

package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ModelExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ModelExecutionStatusRes(

    @Schema(description = "실행 리소스 식별자", example = "exec_a1b2c3d4e5f6")
    String executionId,

    @Schema(description = "시나리오 ID", example = "1")
    Long scenarioId,

    @Schema(description = "외부 모델 측 실행 추적 키", example = "run_20260508_0001")
    String externalRunId,

    @Schema(description = "현재 실행 상태", implementation = ModelExecutionStatus.class, example = "RUNNING")
    ModelExecutionStatus status,

    @Schema(description = "실행 접수 시각")
    LocalDateTime requestedAt,

    @Schema(description = "실행 시작 시각")
    LocalDateTime startedAt,

    @Schema(description = "실행 종료 시각")
    LocalDateTime finishedAt,

    @Schema(description = "실패 시 오류 메시지", example = "Model execution failed due to invalid source data.")
    String errorMessage

) {
}

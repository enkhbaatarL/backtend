package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ModelExecutionAcceptRes(

    @Schema(description = "생성된 시나리오 케이스 ID", example = "101")
    Long caseId,

    @Schema(description = "시나리오 ID", example = "1")
    Long scenarioId,

    @Schema(description = "backend가 발급한 yyyymmdd_hhmmss_XXXX 형식 실행 추적 ID", example = "20260619_143500_A1B2")
    String runId,

    @Schema(description = "실행 접수 직후 상태", implementation = ModelExecutionStatus.class, example = "RUNNING")
    ModelExecutionStatus status,

    @ArraySchema(schema = @Schema(implementation = ModelExecutionFileRes.class))
    List<ModelExecutionFileRes> files,

    @Schema(description = "실행 접수 시각")
    LocalDateTime requestedAt,

    @Schema(description = "실행 시작 시각")
    LocalDateTime startedAt,

    @Schema(description = "상태 polling endpoint", example = "/api/v1/mi/next-gen/scenarios/1/model-executions/20260619_143500_A1B2")
    String statusEndpoint

) {
}

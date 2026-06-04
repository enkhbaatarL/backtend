package com.sk.domain.mi.dto.request.nextgen;

import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ModelExecutionCallbackReq(

    @NotBlank(message = "externalRunId는 필수입니다")
    @Schema(description = "외부 모델 측 실행 추적 키", example = "run_20260508_0001")
    String externalRunId,

    @NotNull(message = "status는 필수입니다")
    @Schema(description = "콜백으로 전달되는 최종 상태", implementation = ModelExecutionStatus.class, example = "SUCCEEDED")
    ModelExecutionStatus status,

    @Schema(description = "외부 모델 실행 종료 시각")
    LocalDateTime finishedAt,

    @Schema(description = "실패 시 외부 모델이 전달하는 오류 메시지", example = "Model execution failed due to invalid source data.")
    String errorMessage

) {
}

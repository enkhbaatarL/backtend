package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FundamentalModelRunStatusRes(

    @Schema(description = "실행 추적 ID", example = "3f7e44c4-2bb5-4b09-b577-920f83ad2a6d")
    String runId,

    @Schema(description = "실행 상태", implementation = FundamentalModelRunStatus.class, example = "RUNNING")
    FundamentalModelRunStatus status,

    @Schema(description = "실행 요청 시각")
    LocalDateTime requestedAt,

    @Schema(description = "실행 시작 시각")
    LocalDateTime startedAt,

    @Schema(description = "완료/실패 시각")
    LocalDateTime finishedAt,

    @Schema(description = "실패 사유", example = "EXTERNAL_ERROR")
    String failureReason,

    @Schema(description = "실패 상세 메시지", example = "Model execution failed due to invalid source data.")
    String errorMessage

) {
}

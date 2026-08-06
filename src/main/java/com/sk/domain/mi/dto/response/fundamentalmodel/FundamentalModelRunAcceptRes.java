package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FundamentalModelRunAcceptRes(

    @Schema(description = "실행 대상 case ID", example = "101")
    Long caseId,

    @Schema(description = "backend가 생성한 yyyymmdd_hhmmss_XXXX 형식 실행 ID", example = "20260521_143500_A1B2")
    String runId,

    @Schema(description = "실행 접수 직후 상태", implementation = FundamentalModelRunStatus.class, example = "RUNNING")
    FundamentalModelRunStatus status,

    @Schema(description = "실행 요청 시각")
    LocalDateTime requestedAt,

    @Schema(description = "실행 시작 시각")
    LocalDateTime startedAt,

    @Schema(description = "case 기준 상태 조회 endpoint")
    String statusEndpoint

) {
}

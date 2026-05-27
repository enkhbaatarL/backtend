package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FundamentalModelRunCallbackRes(

    @Schema(description = "callback 대상 yyyymmdd_hhmmss_XXXX 형식 run ID", example = "20260521_143500_A1B2")
    String runId,

    @Schema(description = "callback 처리 후 run terminal 상태", implementation = FundamentalModelRunStatus.class, example = "COMPLETED")
    FundamentalModelRunStatus status,

    @Schema(description = "backend callback 처리 시각")
    LocalDateTime processedAt

) {
}

package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FundamentalModelRunCallbackRes(

    @Schema(description = "callback 대상 run ID", example = "3f7e44c4-2bb5-4b09-b577-920f83ad2a6d")
    String runId,

    @Schema(description = "연결된 case ID", example = "101")
    Long caseId,

    @Schema(description = "callback 처리 후 run terminal 상태", implementation = FundamentalModelRunStatus.class, example = "COMPLETED")
    FundamentalModelRunStatus status,

    @Schema(description = "backend callback 처리 시각")
    LocalDateTime processedAt,

    @Schema(description = "동일 terminal payload 재전송을 no-op으로 처리했는지 여부", example = "false")
    Boolean idempotentReplay

) {
}

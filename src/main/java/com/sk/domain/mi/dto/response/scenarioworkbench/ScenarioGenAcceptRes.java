package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ScenarioGenStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ScenarioGenAcceptRes(

    @Schema(description = "생성 요청 ID", example = "1")
    Long requestId,

    @Schema(description = "요청 상태", example = "PROCESSING")
    ScenarioGenStatus status,

    @Schema(description = "요청 상태 라벨", example = "생성중")
    String statusLabel,

    @Schema(description = "요청 일시")
    LocalDateTime requestedAt

) {
}

package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ScenarioGenStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ScenarioGenStatusRes(

    @Schema(description = "생성 요청 ID", example = "200")
    Long requestId,

    @Schema(description = "대상 이벤트 ID", example = "3")
    Long eventId,

    @Schema(description = "요청 상태", example = "processing")
    ScenarioGenStatus status,

    @Schema(description = "요청 상태 라벨", example = "생성중")
    String statusLabel

) {
}

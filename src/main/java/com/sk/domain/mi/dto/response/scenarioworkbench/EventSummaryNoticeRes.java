package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;

public record EventSummaryNoticeRes(

    @Schema(description = "신규 이벤트 수", example = "2")
    int newEventCount,

    @Schema(description = "갱신 이벤트 수", example = "5")
    int updatedEventCount

) {
}

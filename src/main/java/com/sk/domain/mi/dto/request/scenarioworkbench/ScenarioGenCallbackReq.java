package com.sk.domain.mi.dto.request.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record ScenarioGenCallbackReq(

    @NotNull(message = "requestId는 필수입니다")
    @Schema(description = "시나리오 생성 요청 ID", example = "200")
    Long requestId,

    @NotNull(message = "success는 필수입니다")
    @Schema(description = "생성 성공 여부", example = "true")
    Boolean success,

    @Schema(description = "생성된 시나리오 ID 목록 (성공 시)", example = "[101, 102, 103]")
    List<Long> scenarioIds,

    @Schema(description = "완료 시각")
    LocalDateTime completedAt,

    @Schema(description = "실패 사유 (실패 시)", example = "External API timeout")
    String failReason

) {
}

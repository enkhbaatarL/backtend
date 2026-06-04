package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 시나리오 판정 상태 변경 결과.
 */
public record ScenarioStatusRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "변경된 판정 상태 (pending/realized/discarded)", example = "realized")
    ScenarioStatus status,

    @Schema(description = "판정 상태 라벨", example = "실현")
    String statusLabel

) {}

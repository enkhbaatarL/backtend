package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 파라미터 단위의 시나리오별 근거.
 */
public record GroupModelParamCompareScenarioRationaleRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "파라미터 단위 근거", nullable = true)
    String rationale,

    @Schema(description = "slot 그룹 단위 근거 (단건 조회의 groupedRationales와 동일 구조)", nullable = true)
    JsonNode groupedRationales
) {
}

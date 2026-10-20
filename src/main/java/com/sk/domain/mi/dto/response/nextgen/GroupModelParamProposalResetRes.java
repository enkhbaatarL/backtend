package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 그룹 파라미터 제안 전체 초기화 결과.
 */
public record GroupModelParamProposalResetRes(

    @Schema(description = "시나리오 그룹 ID", example = "61")
    Long scenarioGroupId,

    @Schema(description = "초기화된 시나리오 ID 목록 (생성 이력이 없거나 이미 초기화된 시나리오는 제외)", example = "[309, 310]")
    List<Long> resetScenarioIds
) {
}

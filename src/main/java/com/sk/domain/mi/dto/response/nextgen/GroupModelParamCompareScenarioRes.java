package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비교 그리드의 시나리오 열 정의. 완료(제안 보유) 시나리오만 포함된다.
 */
public record GroupModelParamCompareScenarioRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "항행 차질과 아프리카 공급 훼손 결합")
    String scenarioName,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "Agent model parameter proposal ID", example = "10001")
    Long modelParamProposalId,

    @Schema(description = "단건 조회와 동일한 표시 상태 (PROPOSED/CONFIRMED/EXECUTING 등)", example = "PROPOSED")
    String status
) {
}

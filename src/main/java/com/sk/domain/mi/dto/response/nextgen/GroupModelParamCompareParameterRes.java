package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 비교 그리드의 파라미터 단위 묶음.
 * values의 row는 단건 조회와 동일하게 slot 축이 flat 필드이며, 시나리오별 값만 cells 배열로 확장된다.
 * row 예시: {"Year":2026,"Month":7,"Country":"Norway","basecaseValue":0.224,"cells":[{"proposedValue":0.3,"rationale":"..."},null]}
 */
public record GroupModelParamCompareParameterRes(

    @Schema(description = "수급모델 파라미터명", example = "SupplyDisruption")
    String parameterName,

    @Schema(description = "Agent payload modelTarget 원본 (첫 등장 시나리오 기준, 단건 조회와 동일 구조)")
    JsonNode modelTarget,

    @Schema(description = "시나리오별 파라미터 단위 근거 (단건 조회의 rationale/groupedRationales를 시나리오별로 담음)")
    List<GroupModelParamCompareScenarioRationaleRes> scenarioRationales,

    @Schema(description = "slot 축 기준 병합 행 배열. slot 축은 단건과 동일한 flat 필드 + basecaseValue + cells(시나리오별, scenarios 순서, 미제안 null)")
    JsonNode values
) {
}

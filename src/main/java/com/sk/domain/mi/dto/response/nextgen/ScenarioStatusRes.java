package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시나리오 판정 상태 변경 결과.
 * '실현' 변경 시 같은 그룹 나머지 시나리오는 '폐기'로 자동 변경되며, 그 대상이 discardedScenarioIds 에 담긴다.
 */
public record ScenarioStatusRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "변경된 판정 상태 (pending/realized/discarded)", example = "realized")
    ScenarioStatus status,

    @Schema(description = "판정 상태 라벨", example = "실현")
    String statusLabel,

    @Schema(description = "'폐기'로 자동 변경된 시나리오 ID 목록 (실현 변경 시에만, 그 외 빈 배열)", example = "[12, 13, 14]")
    List<Long> discardedScenarioIds

) {

    public static ScenarioStatusRes of(Long scenarioId, ScenarioStatus status, List<Long> discardedScenarioIds) {
        return new ScenarioStatusRes(scenarioId, status, status.getLabel(), discardedScenarioIds);
    }

}

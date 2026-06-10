package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 이벤트 상세의 '연관 시나리오'
 */
public record EventRelatedScenariosRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "시나리오 그룹명", example = "미-이란 충돌 및 호르무즈 해협 봉쇄")
    String groupName,

    @Schema(description = "동적 표 헤더 정의 (그룹별 차원 칼럼)")
    List<ScenarioDimensionColumnRes> dimensionColumns,

    @Schema(description = "그룹 내 시나리오 목록")
    List<EventRelatedScenarioRes> scenarios

) {

    public static EventRelatedScenariosRes of(ScenarioGroupBaseRow group, List<ScenarioDimensionColumnRes> dimensionColumns, List<EventRelatedScenarioRes> scenarios) {
        return new EventRelatedScenariosRes(group.scenarioGroupId(), group.title(), dimensionColumns, scenarios);
    }

}

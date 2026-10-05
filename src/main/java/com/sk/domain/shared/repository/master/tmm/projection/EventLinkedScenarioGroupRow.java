package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import java.time.LocalDateTime;

/**
 * 이벤트에 연결된 시나리오 그룹의 평탄화 행 (그룹 x 구성 이벤트).
 * 그룹 1건이 구성 이벤트 수만큼 반복되며, 서비스에서 scenarioGroupId 기준으로 묶어 조립한다.
 */
public record EventLinkedScenarioGroupRow(
    Long scenarioGroupId,
    String groupTitle,
    ScenarioGroupStatus status,
    ScenarioGroupStage stage,
    Long requestUserId,
    LocalDateTime createDt,
    Integer memberDisplayOrder,
    String memberEventName
) {

    @QueryProjection
    public EventLinkedScenarioGroupRow {
    }

}

package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;

/**
 * 이벤트별 시나리오 설계 점유/가능 상태 조회용 projection.
 * <p>이벤트 : 활성(비삭제) 시나리오 그룹 = 1:1 left join.
 * 그룹이 없으면 그룹 관련 필드는 null(= 설계가능). 그룹 status가 creating이면 설계중(점유)이며,
 * 점유자는 그룹의 요청자({@code request_usr_id})다.
 */
public record EventDesignAvailabilityRow(
    Long miEventId,
    Long scenarioGroupId,
    ScenarioGroupStatus scenarioGroupStatus,
    ScenarioGroupStage scenarioGroupStage,
    Long requestUserId,
    boolean templateAvailable
) {

    @QueryProjection
    public EventDesignAvailabilityRow {
    }

}

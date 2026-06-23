package com.sk.domain.shared.repository.master.tmd.projection;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;

/**
 * 시나리오 케이스의 실행 상태 행 (그래프 running/executed 판정용).
 * 시나리오당 케이스가 여러 개일 수 있어 행 단위로 내려오며, running/completed 집계는 호출 측에서 한다.
 */
public record ScenarioRunStateRow(
    Long scenarioId,
    FundamentalModelCaseStatus caseStatus,
    FundamentalModelRunStatus runStatus
) {

    @QueryProjection
    public ScenarioRunStateRow {}

}

package com.sk.domain.shared.repository.master.tmd.projection;

import com.querydsl.core.annotations.QueryProjection;

/**
 * 시나리오별 최신 완료 실행 (재실행 stale 판정용).
 * runId = 케이스·실행이 모두 COMPLETED인 것 중 최신(run_seq 최대) 실행의 run_id.
 * 결과에 포함된 scenarioId = 완료된 수급모델 실행을 가진 시나리오(= executed).
 */
public record ScenarioLatestRunRow(
    Long scenarioId,
    String runId
) {

    @QueryProjection
    public ScenarioLatestRunRow {}

}

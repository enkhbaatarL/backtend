package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시나리오 그룹 그래프 실행상태 폴링 응답 (가격 제외, 경량).
 * 그래프 화면에서 수급모델 실행중/완료를 폴링하기 위한 용도다. 실행 후 이 응답을 폴링하다가
 * {@code anyRunning=false}로 바뀌면 그래프 조회 API를 한 번 재조회해 완료된 가격을 표출한다.
 */
public record ScenarioGroupRunStatusRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "그룹 내 실행중 시나리오 존재 여부", example = "true")
    boolean anyRunning,

    @Schema(description = "시나리오별 실행 상태")
    List<ScenarioRunStatus> scenarios

) {

    public record ScenarioRunStatus(

        @Schema(description = "시나리오 ID", example = "11")
        Long scenarioId,

        @Schema(description = "수급모델 실행중 여부 (RUNNING 케이스 존재). 재조회 트리거: true -> false 전환", example = "false")
        boolean modelRunning

    ) {}

    public static ScenarioGroupRunStatusRes of(Long scenarioGroupId, List<ScenarioRunStatus> scenarios) {
        boolean anyRunning = scenarios.stream().anyMatch(ScenarioRunStatus::modelRunning);
        return new ScenarioGroupRunStatusRes(scenarioGroupId, anyRunning, scenarios);
    }

}

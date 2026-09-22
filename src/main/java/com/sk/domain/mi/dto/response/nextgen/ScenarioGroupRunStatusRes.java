package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시나리오 그룹 그래프 실행상태 폴링 응답 (가격 제외, 경량).
 * 그래프 화면에서 수급모델 실행 접수/실행중/완료를 폴링하기 위한 용도다. 실행 후 이 응답을 폴링하다가
 * anyRunning과 anyPreparing이 모두 false로 바뀌면 그래프 조회 API를 한 번 재조회해 완료된 가격을 표출한다.
 * preparing은 실행 접수 후 아직 RUNNING 케이스가 생성되기 전 준비 구간(파라미터 확정 반영, 엑셀 병합, 파일 스테이징)을 뜻한다.
 */
public record ScenarioGroupRunStatusRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "그룹 내 실행중 시나리오 존재 여부", example = "true")
    boolean anyRunning,

    @Schema(description = "그룹 내 실행 준비중(접수 후 RUNNING 전) 시나리오 존재 여부", example = "false")
    boolean anyPreparing,

    @Schema(description = "시나리오별 실행 상태")
    List<ScenarioRunStatus> scenarios

) {

    public record ScenarioRunStatus(

        @Schema(description = "시나리오 ID", example = "11")
        Long scenarioId,

        @Schema(description = "수급모델 실행중 여부 (RUNNING 케이스 존재). 재조회 트리거: true -> false 전환", example = "false")
        boolean modelRunning,

        @Schema(description = "수급모델 실행 준비중 여부 (접수됨, 엑셀 병합 등으로 아직 RUNNING 케이스 생성 전)", example = "false")
        boolean preparing

    ) {}

    public static ScenarioGroupRunStatusRes of(Long scenarioGroupId, List<ScenarioRunStatus> scenarios) {
        boolean anyRunning = scenarios.stream().anyMatch(ScenarioRunStatus::modelRunning);
        boolean anyPreparing = scenarios.stream().anyMatch(ScenarioRunStatus::preparing);
        return new ScenarioGroupRunStatusRes(scenarioGroupId, anyRunning, anyPreparing, scenarios);
    }

}

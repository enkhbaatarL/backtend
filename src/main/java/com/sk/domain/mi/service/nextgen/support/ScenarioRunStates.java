package com.sk.domain.mi.service.nextgen.support;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioRunStateRow;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 그룹 시나리오들의 실행 상태 집계
 */
public record ScenarioRunStates(
    Set<Long> running,
    Set<Long> completed
) {

    /**
     * - running = RUNNING 케이스 존재 (modelRunning)
     * - completed = 케이스 상태 마지막 실행 상태 모두 COMPLETED (executed)
     */
    public static ScenarioRunStates from(List<ScenarioRunStateRow> rows) {

        Set<Long> running = rows.stream()
            .filter(row -> row.runStatus() == FundamentalModelRunStatus.RUNNING)
            .map(ScenarioRunStateRow::scenarioId)
            .collect(Collectors.toUnmodifiableSet());

        Set<Long> completed = rows.stream()
            .filter(row -> row.caseStatus() == FundamentalModelCaseStatus.COMPLETED && row.runStatus() == FundamentalModelRunStatus.COMPLETED)
            .map(ScenarioRunStateRow::scenarioId)
            .collect(Collectors.toUnmodifiableSet());

        return new ScenarioRunStates(running, completed);
    }

    public boolean isRunning(Long scenarioId) {
        return running.contains(scenarioId);
    }

}

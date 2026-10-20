package com.sk.domain.shared.enums.nextgen;

import java.util.Set;

public enum ModelExecutionPreparationStatus {

    REQUESTING,
    PROPOSAL_PENDING,
    REQUEST_FAILED,
    REQUEST_NO_CHANGES,
    REQUEST_INTERRUPTED,
    CONFIRMING,
    PREPARING_EXECUTION,
    EXECUTING,
    EXECUTION_FAILED,
    /** 그룹 전체 초기화 표시 행. 이전 행은 그대로 두고 이 행이 최신이 되어 시나리오를 미생성 상태로 되돌린다 */
    RESET;

    private static final Set<ModelExecutionPreparationStatus> IN_FLIGHT = Set.of(
        REQUESTING,
        PROPOSAL_PENDING,
        CONFIRMING,
        PREPARING_EXECUTION
    );

    public boolean isInFlight() {
        return IN_FLIGHT.contains(this);
    }
}

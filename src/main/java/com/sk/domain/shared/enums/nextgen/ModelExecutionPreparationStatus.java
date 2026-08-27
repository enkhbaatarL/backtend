package com.sk.domain.shared.enums.nextgen;

import java.util.Set;

public enum ModelExecutionPreparationStatus {

    REQUESTING,
    PROPOSAL_PENDING,
    REQUEST_FAILED,
    REQUEST_NO_CHANGES,
    CONFIRMING,
    PREPARING_EXECUTION,
    EXECUTING,
    EXECUTION_FAILED;

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

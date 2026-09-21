package com.sk.domain.shared.enums.nextgen;

public enum CurrentModelParamProposalState {

    NOT_REQUESTED,
    GENERATING,
    PROPOSAL_READY,
    CONFIRMED_PENDING_EXECUTION,
    EXECUTING,
    EXECUTED_CONFIRMED,
    PROPOSAL_FAILED,
    PROPOSAL_NO_CHANGES,
    PROPOSAL_RETRYABLE,
    EXECUTION_FAILED,
    ABSTAINED
}

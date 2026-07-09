package com.sk.domain.shared.client.dto.response;

import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;

public record AgentModelParamProposalCreateRes(

    String status,
    String message,
    Data data
) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    public record Data(
        Long modelParamProposalId,
        AgentModelParamProposalStatus status
    ) {
    }
}

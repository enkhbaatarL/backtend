package com.sk.domain.mi.dto.request.nextgen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;
import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelParamProposalCallbackReq(
    @NotNull
    AgentCallbackResult status,
    @NotBlank
    String message,
    @Valid
    Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
        Long modelParamProposalId,
        Long scenarioId,
        Long scenarioGroupId,
        Long eventId,
        AgentModelParamProposalStatus status,
        JsonNode proposalParameters
    ) {
    }
}

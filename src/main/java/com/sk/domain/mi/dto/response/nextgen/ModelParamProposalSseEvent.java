package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelParamProposalSseEvent(
    Long scenarioId,
    Long modelParamProposalId,
    String status
) {

    public static ModelParamProposalSseEvent of(Long scenarioId, Long modelParamProposalId, String status) {
        return new ModelParamProposalSseEvent(scenarioId, modelParamProposalId, status);
    }
}

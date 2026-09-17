package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 수급모델 파라미터 제안 완료/실패 SSE 신호.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelParamProposalSseEvent(
    Long scenarioId,
    Long scenarioGroupId,
    Long modelParamProposalId,
    String status
) {

    public static ModelParamProposalSseEvent of(Long scenarioId, Long scenarioGroupId, Long modelParamProposalId, String status) {
        return new ModelParamProposalSseEvent(scenarioId, scenarioGroupId, modelParamProposalId, status);
    }
}

package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.AgentModelParamConfirmationReq;
import com.sk.domain.shared.client.dto.response.AgentModelParamConfirmationRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ModelParamProposalAgentApi {

    @PostExchange("/model-parameters/scenarios/{scenarioId}/proposals")
    String createProposal(@PathVariable Long scenarioId);

    @PostExchange("/model-parameters/proposals/{modelParamProposalId}/confirmation")
    AgentModelParamConfirmationRes confirmProposal(
        @PathVariable Long modelParamProposalId,
        @RequestBody AgentModelParamConfirmationReq req
    );
}

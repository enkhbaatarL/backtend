package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.response.nextgen.ModelExecutionOptionsRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalRes;
import com.sk.domain.shared.client.dto.request.ModelParamPayloadItem;
import com.sk.domain.shared.client.dto.response.AgentModelParamConfirmationRes;
import java.util.List;

public interface ModelParamProposalService {

    ModelExecutionOptionsRes getModelExecutionOptions(Long scenarioId);

    ModelParamProposalAcceptRes createProposal(Long scenarioId);

    ModelParamProposalRes getProposal(Long scenarioId, Long modelParamProposalId);

    AgentModelParamConfirmationRes confirmProposal(Long modelParamProposalId, List<ModelParamPayloadItem> confirmationParameters);
}

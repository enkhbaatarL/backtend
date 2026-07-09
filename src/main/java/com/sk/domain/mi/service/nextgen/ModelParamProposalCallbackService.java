package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalCallbackReq;

public interface ModelParamProposalCallbackService {

    void handleCallback(Long scenarioId, ModelParamProposalCallbackReq request);
}

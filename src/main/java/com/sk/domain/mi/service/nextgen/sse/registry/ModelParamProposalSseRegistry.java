package com.sk.domain.mi.service.nextgen.sse.registry;

import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalSseEvent;
import org.springframework.stereotype.Component;

@Component
public class ModelParamProposalSseRegistry extends AbstractSseRegistry<ModelParamProposalSseEvent> {

    @Override
    protected String eventName(ModelParamProposalSseEvent event) {
        return "model-parameter-proposal";
    }

    @Override
    protected String logTag() {
        return "[ModelParamProposal SSE]";
    }

    @Override
    protected String connectionIdName() {
        return "scenarioId";
    }
}

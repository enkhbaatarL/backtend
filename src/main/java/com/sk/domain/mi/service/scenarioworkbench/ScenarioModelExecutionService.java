package com.sk.domain.mi.service.scenarioworkbench;

import com.sk.domain.mi.dto.request.scenarioworkbench.ModelExecutionCallbackReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ModelExecutionSubmitReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ModelExecutionStatusRes;

public interface ScenarioModelExecutionService {

    ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId);

    ModelExecutionAcceptRes submitModelExecution(Long scenarioId, ModelExecutionSubmitReq request);

    ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String executionId);

    ModelExecutionStatusRes updateModelExecutionFromCallback(
        Long scenarioId,
        String executionId,
        ModelExecutionCallbackReq request
    );
}

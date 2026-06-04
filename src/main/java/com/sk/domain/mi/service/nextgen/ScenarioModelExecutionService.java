package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.ModelExecutionCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.ModelExecutionSubmitReq;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;

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

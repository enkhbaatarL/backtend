package com.sk.domain.mi.service.nextgen.mock;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.ModelExecutionCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.ModelExecutionSubmitReq;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionService;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class ScenarioModelExecutionServiceDelegator implements ScenarioModelExecutionService {

    private final ScenarioModelExecutionServiceImpl realService;
    private final ScenarioModelExecutionMockService mockService;
    private final NextGenMockProperties mockProps;

    @Override
    public ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId) {
        return mockProps.getModelExecutions().getApi().isGetParameters()
            ? mockService.getModelExecutionParameters(scenarioId)
            : realService.getModelExecutionParameters(scenarioId);
    }

    @Override
    public ModelExecutionAcceptRes submitModelExecution(Long scenarioId, ModelExecutionSubmitReq request) {
        return mockProps.getModelExecutions().getApi().isSubmit()
            ? mockService.submitModelExecution(scenarioId, request)
            : realService.submitModelExecution(scenarioId, request);
    }

    @Override
    public ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String executionId) {
        return mockProps.getModelExecutions().getApi().isGetStatus()
            ? mockService.getModelExecutionStatus(scenarioId, executionId)
            : realService.getModelExecutionStatus(scenarioId, executionId);
    }

    @Override
    public ModelExecutionStatusRes updateModelExecutionFromCallback(
        Long scenarioId,
        String executionId,
        ModelExecutionCallbackReq request
    ) {
        return mockProps.getModelExecutions().getApi().isCallback()
            ? mockService.updateModelExecutionFromCallback(scenarioId, executionId, request)
            : realService.updateModelExecutionFromCallback(scenarioId, executionId, request);
    }
}

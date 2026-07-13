package com.sk.domain.mi.service.nextgen.mock;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalExecutionReq;
import com.sk.domain.mi.dto.response.nextgen.LatestModelParamProposalParametersRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionService;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionServiceImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
@RequiredArgsConstructor
public class ScenarioModelExecutionServiceDelegator implements ScenarioModelExecutionService {

    private final ScenarioModelExecutionServiceImpl realService;
    private final ScenarioModelExecutionMockService mockService;
    private final NextGenMockProperties mockProps;

    @Override
    public ModelExecutionParameterRes getModelExecutionParameters(Long scenarioId) {
        return realService.getModelExecutionParameters(scenarioId);
    }

    @Override
    public ModelExecutionAcceptRes submitModelExecution(Long scenarioId, List<MultipartFile> files) {
        return mockProps.getModelExecutions().getApi().isSubmit()
            ? mockService.submitModelExecution(scenarioId, files)
            : realService.submitModelExecution(scenarioId, files);
    }

    @Override
    public ModelExecutionAcceptRes submitModelExecution(Long scenarioId, ModelParamProposalExecutionReq request) {
        return realService.submitModelExecution(scenarioId, request);
    }

    @Override
    public ModelExecutionStatusRes getModelExecutionStatus(Long scenarioId, String runId) {
        return mockProps.getModelExecutions().getApi().isGetStatus()
            ? mockService.getModelExecutionStatus(scenarioId, runId)
            : realService.getModelExecutionStatus(scenarioId, runId);
    }

    @Override
    public LatestModelParamProposalParametersRes getLatestConfirmedParameters(Long scenarioId) {
        return realService.getLatestConfirmedParameters(scenarioId);
    }

    @Override
    public LatestModelParamProposalParametersRes getLatestProposedParameters(Long scenarioId) {
        return realService.getLatestProposedParameters(scenarioId);
    }
}

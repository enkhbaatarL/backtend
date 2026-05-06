package com.sk.domain.mi.service.scenarioworkbench.mock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sk.domain.mi.config.ScenarioWorkbenchMockProperties;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioListRes;
import com.sk.domain.mi.service.scenarioworkbench.ScenarioService;
import com.sk.domain.mi.service.scenarioworkbench.ScenarioServiceImpl;

import lombok.RequiredArgsConstructor;

@Service
@Primary
@RequiredArgsConstructor
public class ScenarioServiceDelegator implements ScenarioService {

    private final ScenarioServiceImpl realService;
    private final ScenarioMockService mockService;
    private final ScenarioWorkbenchMockProperties mockProps;

    @Override
    public ScenarioListRes getScenarios(ScenarioListReq request) {
        return mockProps.getScenarios().getApi().isGetList()
            ? mockService.getScenarios(request)
            : realService.getScenarios(request);
    }

    @Override
    public ScenarioDetailRes getScenarioDetail(Long scenarioId) {
        return mockProps.getScenarios().getApi().isGetDetail()
            ? mockService.getScenarioDetail(scenarioId)
            : realService.getScenarioDetail(scenarioId);
    }

}

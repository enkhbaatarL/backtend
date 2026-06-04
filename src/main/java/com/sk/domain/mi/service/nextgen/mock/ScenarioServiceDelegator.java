package com.sk.domain.mi.service.nextgen.mock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.ScenarioListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioListRes;
import com.sk.domain.mi.service.nextgen.ScenarioService;
import com.sk.domain.mi.service.nextgen.ScenarioServiceImpl;

import lombok.RequiredArgsConstructor;

@Service
@Primary
@RequiredArgsConstructor
public class ScenarioServiceDelegator implements ScenarioService {

    private final ScenarioServiceImpl realService;
    private final ScenarioMockService mockService;
    private final NextGenMockProperties mockProps;

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

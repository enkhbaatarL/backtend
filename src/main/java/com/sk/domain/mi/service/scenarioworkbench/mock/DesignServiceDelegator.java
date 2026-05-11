package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.sk.domain.mi.config.ScenarioWorkbenchMockProperties;
import com.sk.domain.mi.dto.request.scenarioworkbench.DesignEventListReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenCallbackReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.DesignEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.GeneratedScenarioRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenAcceptRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenStatusRes;
import com.sk.domain.mi.service.scenarioworkbench.DesignService;
import com.sk.domain.mi.service.scenarioworkbench.DesignServiceImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class DesignServiceDelegator implements DesignService {

    private final DesignServiceImpl realService;
    private final DesignMockService mockService;
    private final ScenarioWorkbenchMockProperties mockProps;

    @Override
    public DesignEventListRes getDesignEvents(DesignEventListReq request) {
        return mockProps.getDesign().getApi().isGetEvents()
            ? mockService.getDesignEvents(request) : realService.getDesignEvents(request);
    }

    @Override
    public ScenarioGenAcceptRes generateScenarios(ScenarioGenReq request) {
        return mockProps.getDesign().getApi().isPostGenerate()
            ? mockService.generateScenarios(request) : realService.generateScenarios(request);
    }

    @Override
    public void handleCallback(ScenarioGenCallbackReq request) {
        if (mockProps.getDesign().getApi().isPostCallback()) {
            mockService.handleCallback(request);
        } else {
            realService.handleCallback(request);
        }
    }

    @Override
    public List<GeneratedScenarioRes> getGeneratedScenarios(Long requestId) {
        return mockProps.getDesign().getApi().isGetGeneratedScenarios()
            ? mockService.getGeneratedScenarios(requestId) : realService.getGeneratedScenarios(requestId);
    }

    @Override
    public List<ScenarioGenStatusRes> getGenerateStatuses(List<Long> requestIds) {
        return mockProps.getDesign().getApi().isGetStatuses()
            ? mockService.getGenerateStatuses(requestIds) : realService.getGenerateStatuses(requestIds);
    }

    @Override
    public void reset() {
        mockService.reset();
    }

}

package com.sk.domain.mi.service.nextgen.mock;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.DesignEventListReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGenCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGenReq;
import com.sk.domain.mi.dto.response.nextgen.DesignEventListRes;
import com.sk.domain.mi.dto.response.nextgen.GeneratedScenarioRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGenAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGenStatusRes;
import com.sk.domain.mi.service.nextgen.DesignService;
import com.sk.domain.mi.service.nextgen.DesignServiceImpl;
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
    private final NextGenMockProperties mockProps;

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

package com.sk.domain.mi.service.nextgen.mock;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStageRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStateRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDimensionTemplateRes;
import com.sk.domain.mi.service.nextgen.ScenarioDesignService;
import com.sk.domain.mi.service.nextgen.ScenarioDesignServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class ScenarioDesignServiceDelegator implements ScenarioDesignService {

    private final ScenarioDesignServiceImpl realService;
    private final ScenarioDesignMockService mockService;
    private final NextGenMockProperties mockProps;

    private NextGenMockProperties.Design.Api api() {
        return mockProps.getDesign().getApi();
    }

    @Override
    public ScenarioDesignStageRes startDesign(ScenarioDesignStartReq request) {
        return api().isStart()
            ? mockService.startDesign(request)
            : realService.startDesign(request);
    }

    @Override
    public ScenarioDesignStateRes getDesign(Long scenarioGroupId) {
        return api().isGetState()
            ? mockService.getDesign(scenarioGroupId)
            : realService.getDesign(scenarioGroupId);
    }

    @Override
    public ScenarioDimensionTemplateRes getDimensionTemplates(Long scenarioGroupId) {
        // mock=실DB 템플릿을 event_type 기준으로(세션 연동), real=DB 그룹 기준
        return api().isGetDimensionTemplates()
            ? mockService.getDimensionTemplates(scenarioGroupId)
            : realService.getDimensionTemplates(scenarioGroupId);
    }

    @Override
    public ScenarioDesignStageRes validateDesign(Long scenarioGroupId, ScenarioValidationReq request) {
        return api().isValidate()
            ? mockService.validate(scenarioGroupId, request)
            : realService.validateDesign(scenarioGroupId, request);
    }

    @Override
    public ScenarioDesignStageRes createSignposts(Long scenarioGroupId, ScenarioSignpostsReq request) {
        return api().isCreateSignposts()
            ? mockService.createSignposts(scenarioGroupId, request)
            : realService.createSignposts(scenarioGroupId, request);
    }

    @Override
    public ScenarioDesignStageRes confirmDesign(Long scenarioGroupId, ScenarioConfirmationReq request) {
        return api().isConfirm()
            ? mockService.confirm(scenarioGroupId, request)
            : realService.confirmDesign(scenarioGroupId, request);
    }

    @Override
    public ScenarioDesignStateRes reset(Long scenarioGroupId) {
        return api().isReset()
            ? mockService.reset(scenarioGroupId)
            : realService.reset(scenarioGroupId);
    }

}

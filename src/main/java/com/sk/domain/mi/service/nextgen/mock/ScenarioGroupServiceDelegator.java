package com.sk.domain.mi.service.nextgen.mock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioBulkStatusRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupServiceImpl;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import java.io.ByteArrayInputStream;

import lombok.RequiredArgsConstructor;

@Service
@Primary
@RequiredArgsConstructor
public class ScenarioGroupServiceDelegator implements ScenarioGroupService {

    private final ScenarioGroupServiceImpl realService;
    private final ScenarioGroupMockService mockService;
    private final NextGenMockProperties mockProps;

    @Override
    public ScenarioGroupListRes getScenarioGroups(ScenarioGroupListReq request) {
        return mockProps.getScenarioGroups().getApi().isGetList()
            ? mockService.getScenarioGroups(request)
            : realService.getScenarioGroups(request);
    }

    @Override
    public ScenarioIndividualListRes getScenarioGroupScenarios(ScenarioGroupListReq request) {
        return mockProps.getScenarioGroups().getApi().isGetScenarios()
            ? mockService.getScenarioGroupScenarios(request)
            : realService.getScenarioGroupScenarios(request);
    }

    @Override
    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName) {
        return mockProps.getScenarioGroups().getApi().isGetGraph()
            ? mockService.getScenarioGroupGraph(scenarioGroupId, indexName)
            : realService.getScenarioGroupGraph(scenarioGroupId, indexName);
    }

    @Override
    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId) {
        return mockProps.getScenarioGroups().getApi().isGetGraphExport()
            ? mockService.exportScenarioGroupGraph(scenarioGroupId)
            : realService.exportScenarioGroupGraph(scenarioGroupId);
    }

    @Override
    public ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId) {
        return mockProps.getScenarioGroups().getApi().isGetDetail()
            ? mockService.getScenarioGroupDetail(scenarioGroupId)
            : realService.getScenarioGroupDetail(scenarioGroupId);
    }

    @Override
    public ScenarioDetailViewRes getScenarioDetail(Long scenarioGroupId, Long scenarioId) {
        return mockProps.getScenarioGroups().getApi().isGetScenarioDetail()
            ? mockService.getScenarioDetail(scenarioGroupId, scenarioId)
            : realService.getScenarioDetail(scenarioGroupId, scenarioId);
    }

    @Override
    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioStatus status) {
        return mockProps.getScenarioGroups().getApi().isUpdateScenarioStatus()
            ? mockService.updateScenarioStatus(scenarioGroupId, scenarioId, status)
            : realService.updateScenarioStatus(scenarioGroupId, scenarioId, status);
    }

    @Override
    public ScenarioBulkStatusRes bulkUpdateScenarioStatus(Long scenarioGroupId, ScenarioStatus status) {
        return mockProps.getScenarioGroups().getApi().isBulkUpdateScenarioStatus()
            ? mockService.bulkUpdateScenarioStatus(scenarioGroupId, status)
            : realService.bulkUpdateScenarioStatus(scenarioGroupId, status);
    }

    @Override
    public void deleteScenarioGroup(Long scenarioGroupId) {
        if (mockProps.getScenarioGroups().getApi().isDeleteGroup()) {
            mockService.deleteScenarioGroup(scenarioGroupId);
        } else {
            realService.deleteScenarioGroup(scenarioGroupId);
        }
    }

}

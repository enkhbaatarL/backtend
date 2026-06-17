package com.sk.domain.mi.service.nextgen.mock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.config.NextGenMockProperties;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupServiceImpl;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import java.io.ByteArrayInputStream;

import lombok.RequiredArgsConstructor;

@Service
@Primary
@RequiredArgsConstructor
public class ScenarioGroupServiceDelegator implements ScenarioGroupService {

    private final ScenarioGroupServiceImpl realService;
    private final ScenarioGroupMockService mockService;
    private final ScenarioDesignMockService designMockService;
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
    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit) {
        return mockProps.getScenarioGroups().getApi().isGetGraph()
            ? mockService.getScenarioGroupGraph(scenarioGroupId, indexName, periodUnit)
            : realService.getScenarioGroupGraph(scenarioGroupId, indexName, periodUnit);
    }

    @Override
    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId, GraphPeriodUnit periodUnit) {
        return mockProps.getScenarioGroups().getApi().isGetGraphExport()
            ? mockService.exportScenarioGroupGraph(scenarioGroupId, periodUnit)
            : realService.exportScenarioGroupGraph(scenarioGroupId, periodUnit);
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
    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioDisplayStatus status) {
        return mockProps.getScenarioGroups().getApi().isUpdateScenarioStatus()
            ? mockService.updateScenarioStatus(scenarioGroupId, scenarioId, status)
            : realService.updateScenarioStatus(scenarioGroupId, scenarioId, status);
    }

    @Override
    public void deleteScenarioGroup(Long scenarioGroupId) {
        if (mockProps.getScenarioGroups().getApi().isDeleteGroup()) {
            // mock: 그룹 soft delete + 설계 세션 제거를 함께 수행한다.
            // 세션 제거 → 해당 이벤트가 다시 '설계가능'으로 풀린다(이벤트↔그룹↔설계 유기적 연동).
            // 미확정 설계 취소(그룹 미생성, 세션만 존재)도 같은 삭제 API로 들어오므로 둘 중 하나만 있어도 성공 처리한다.
            boolean sessionRemoved = designMockService.removeSessionByGroupId(scenarioGroupId);
            boolean groupRemoved = mockService.deleteScenarioGroupIfPresent(scenarioGroupId);
            if (!sessionRemoved && !groupRemoved) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "Scenario group not found: " + scenarioGroupId);
            }
        } else {
            realService.deleteScenarioGroup(scenarioGroupId);
        }
    }

}

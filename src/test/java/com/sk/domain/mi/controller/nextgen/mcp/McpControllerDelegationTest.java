package com.sk.domain.mi.controller.nextgen.mcp;

import static org.mockito.BDDMockito.then;

import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.service.nextgen.CmeBenchmarkService;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.mi.service.nextgen.PriceInterpretationService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("MCP Controller 기존 Service 위임 테스트")
@ExtendWith(MockitoExtension.class)
class McpControllerDelegationTest {

    @Mock
    private EventService eventService;

    @Mock
    private ScenarioGroupService scenarioGroupService;

    @Mock
    private PriceInterpretationService priceInterpretationService;

    @Mock
    private CmeBenchmarkService cmeBenchmarkService;

    private McpEventController eventController;
    private McpScenarioGroupController scenarioGroupController;
    private McpScenarioGroupDedicatedController dedicatedController;

    @BeforeEach
    void setUp() {
        eventController = new McpEventController(eventService);
        scenarioGroupController = new McpScenarioGroupController(scenarioGroupService, priceInterpretationService);
        dedicatedController = new McpScenarioGroupDedicatedController(cmeBenchmarkService);
    }

    @Test
    @DisplayName("이벤트 GET 5건은 원본 EventService 메서드와 파라미터를 그대로 사용한다")
    void delegatesAllEventOperations() {
        EventListReq request = new EventListReq(null, null, null, null, null, null, null, null, null);

        eventController.getEventTypesL1();
        eventController.getEvents(request);
        eventController.getEventDetail(10L);
        eventController.getEventOutlooks(10L, 2, 30);
        eventController.getEventRecords(10L, 3, 40);

        then(eventService).should().getEventTypesL1();
        then(eventService).should().getEvents(request);
        then(eventService).should().getEventDetail(10L);
        then(eventService).should().getEventOutlooks(10L, 2, 30);
        then(eventService).should().getEventRecords(10L, 3, 40);
        then(eventService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("시나리오 그룹 GET 6건은 원본 Service 메서드와 파라미터를 그대로 사용한다")
    void delegatesAllScenarioGroupOperations() {
        ScenarioGroupListReq request = new ScenarioGroupListReq(null, null, null, null, null, null);

        scenarioGroupController.getScenarioGroups(request);
        scenarioGroupController.getScenarioGroupScenarios(request);
        scenarioGroupController.getScenarioGroupGraph(20L, IndexName.JKM, GraphPeriodUnit.MONTHLY);
        scenarioGroupController.getScenarioGroupDetail(20L);
        scenarioGroupController.getScenarioDetail(20L, 21L);
        scenarioGroupController.getPriceInterpretations(20L);

        then(scenarioGroupService).should().getScenarioGroups(request);
        then(scenarioGroupService).should().getScenarioGroupScenarios(request);
        then(scenarioGroupService).should().getScenarioGroupGraph(20L, IndexName.JKM, GraphPeriodUnit.MONTHLY);
        then(scenarioGroupService).should().getScenarioGroupDetail(20L);
        then(scenarioGroupService).should().getScenarioDetail(20L, 21L);
        then(priceInterpretationService).should().getInterpretations(20L);
        then(scenarioGroupService).shouldHaveNoMoreInteractions();
        then(priceInterpretationService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("CME GET 1건은 datasource 로직 복제 없이 원본 CmeBenchmarkService에 위임한다")
    void delegatesCmeOperation() {
        dedicatedController.getScenarioGroupGraphCmeBenchmark(IndexName.TTF, GraphPeriodUnit.YEARLY);

        then(cmeBenchmarkService).should().getCmeBenchmarkGraph(IndexName.TTF, GraphPeriodUnit.YEARLY);
        then(cmeBenchmarkService).shouldHaveNoMoreInteractions();
    }
}

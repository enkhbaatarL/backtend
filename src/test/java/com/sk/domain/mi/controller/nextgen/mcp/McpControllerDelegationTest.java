package com.sk.domain.mi.controller.nextgen.mcp;

import static org.mockito.BDDMockito.then;

import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotLookupReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotOutlookListReq;
import com.sk.domain.mi.dto.request.nextgen.snapshot.EventSnapshotRecordListReq;
import com.sk.domain.mi.service.nextgen.CmeBenchmarkService;
import com.sk.domain.mi.service.nextgen.EventService;
import com.sk.domain.mi.service.nextgen.EventSnapshotService;
import com.sk.domain.mi.service.nextgen.PriceInterpretationService;
import com.sk.domain.mi.service.nextgen.ScenarioGroupService;
import com.sk.domain.shared.enums.nextgen.EventSnapshotCompareTarget;
import com.sk.domain.shared.enums.nextgen.EventSnapshotUpdateTrigger;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Mock
    // [MCP Snapshot] 이벤트 Snapshot 컨트롤러 위임 대상
    private EventSnapshotService eventSnapshotService;

    private McpEventDedicatedController eventController;
    private McpScenarioGroupDedicatedController scenarioGroupController;
    private McpEventSnapshotDedicatedController eventSnapshotController;

    @BeforeEach
    void setUp() {
        eventController = new McpEventDedicatedController(eventService);
        scenarioGroupController = new McpScenarioGroupDedicatedController(
            scenarioGroupService, priceInterpretationService, cmeBenchmarkService);
        eventSnapshotController = new McpEventSnapshotDedicatedController(eventSnapshotService);
    }

    @Test
    @DisplayName("이벤트 GET 5건은 원본 EventService 메서드와 파라미터를 그대로 사용한다")
    void delegatesAllEventOperations() {
        EventListReq request = new EventListReq(null, null, null, null, null, null, null, null, null, null, null);

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
        ScenarioGroupListReq request = new ScenarioGroupListReq(null, null, null, null, null, null, null);

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
        scenarioGroupController.getScenarioGroupGraphCmeBenchmark(IndexName.TTF, GraphPeriodUnit.YEARLY);

        then(cmeBenchmarkService).should().getCmeBenchmarkGraph(IndexName.TTF, GraphPeriodUnit.YEARLY);
        then(cmeBenchmarkService).shouldHaveNoMoreInteractions();
    }

    @Test
    // [MCP Snapshot] 신규 컨트롤러 6개 메서드의 위임 확인
    @DisplayName("이벤트 Snapshot GET 6건은 EventSnapshotService 에 파라미터를 그대로 넘긴다")
    void delegatesAllEventSnapshotOperations() {
        EventSnapshotListReq listRequest = new EventSnapshotListReq(
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), EventSnapshotUpdateTrigger.RECORD_INGESTION, "system", 2, 5);
        EventSnapshotLookupReq lookupRequest = new EventSnapshotLookupReq(null, LocalDateTime.of(2026, 9, 15, 14, 0));
        EventSnapshotRecordListReq recordRequest = new EventSnapshotRecordListReq("NEWS", "Reuters", null, null, 1, 10);
        EventSnapshotOutlookListReq outlookRequest = new EventSnapshotOutlookListReq("IEA", null, null, 1, 10);

        eventSnapshotController.getEventSnapshots(30L, listRequest);
        eventSnapshotController.lookupEventSnapshot(30L, lookupRequest);
        eventSnapshotController.getEventSnapshot(30L, 31L);
        eventSnapshotController.compareEventSnapshot(30L, 31L, EventSnapshotCompareTarget.CURRENT);
        eventSnapshotController.getEventSnapshotRecords(30L, 31L, recordRequest);
        eventSnapshotController.getEventSnapshotOutlooks(30L, 31L, outlookRequest);

        then(eventSnapshotService).should().getEventSnapshots(30L, listRequest);
        then(eventSnapshotService).should().lookupEventSnapshot(30L, lookupRequest);
        then(eventSnapshotService).should().getEventSnapshot(30L, 31L);
        then(eventSnapshotService).should().compareEventSnapshot(30L, 31L, EventSnapshotCompareTarget.CURRENT);
        then(eventSnapshotService).should().getEventSnapshotRecords(30L, 31L, recordRequest);
        then(eventSnapshotService).should().getEventSnapshotOutlooks(30L, 31L, outlookRequest);
        then(eventSnapshotService).shouldHaveNoMoreInteractions();
    }
}

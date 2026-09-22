package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ChartWindow;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupAppliedFilterRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupRunStatusRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupRunStatusRes.ScenarioRunStatus;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostMonitoringRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignEvidence;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.EventBundle;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationStore;
import com.sk.domain.mi.service.nextgen.support.IndexGraph;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignAgentMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.mi.service.nextgen.support.ScenarioRunStates;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupEvent;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.entity.master.tmm.MiSignpost;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioApiVersion;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.projection.MiBasePriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioIndividualBaseRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioGroupServiceImpl implements ScenarioGroupService {

    private static final String SCENARIO_CANCELLED = "SCENARIO_CANCELLED";

    /**
     * 수급모델 실행 접수 후 아직 RUNNING 케이스가 생성되기 전 준비 구간 상태.
     * CONFIRMING(파라미터 확정 반영 중), PREPARING_EXECUTION(엑셀 병합, 파일 스테이징 중)만 해당한다.
     * REQUESTING, PROPOSAL_PENDING은 파라미터 제안 생성 단계라 실행 접수와 무관하고,
     * EXECUTING은 실행 콜백 이후에도 유지되는 준종결 상태라 포함하면 안 된다.
     */
    private static final List<ModelExecutionPreparationStatus> MODEL_EXECUTION_PREPARING_STATUSES = List.of(
        ModelExecutionPreparationStatus.CONFIRMING,
        ModelExecutionPreparationStatus.PREPARING_EXECUTION
    );

    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final MiScenarioGroupEventRepository scenarioGroupEventRepository;
    private final ScenarioDesignAgentApi scenarioDesignAgentApi;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final AuthorizationService authorizationService;
    private final PriceInterpretationStore priceInterpretationStore;

    /**
     * [그룹 모드 목록]
     * - 최신순 정렬/기간 필터 모두 그룹 생성일시(create_dt) 기준
     * - adjustedProbability는 조회 시점 계산값(비영속) - 폐기 제외 비례 재분배로 활성 시나리오 합 100, 폐기는 0
     */
    @Override
    public ScenarioGroupListRes getScenarioGroups(ScenarioGroupListReq request) {

        Page<ScenarioGroupBaseRow> page = scenarioGroupRepository.findScenarioGroupPage(request, resolveMineUserId(request));
        List<ScenarioGroupBaseRow> bases = page.getContent();

        List<Long> groupIds = bases.stream().map(ScenarioGroupBaseRow::scenarioGroupId).toList();
        List<Long> eventTypeL2Ids = bases.stream()
            .map(ScenarioGroupBaseRow::eventTypeL2Id)
            .distinct()
            .toList();

        // 그룹별 시나리오 (displayOrder 순) + event_type_l2 별 dimension template 일괄 조회
        Map<Long, List<ScenarioBaseRow>> scenariosByGroup = scenarioGroupRepository.findScenariosByGroupIds(groupIds).stream()
            .collect(Collectors.groupingBy(ScenarioBaseRow::scenarioGroupId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, GroupDimensionTemplate> templates = ScenarioDimensionMapper.parseTemplates(
            scenarioGroupRepository.findTemplatesByEventTypeL2Ids(eventTypeL2Ids)
        );

        Map<Long, ScenarioGroupListItemRes> itemsByGroupId = new LinkedHashMap<>();
        for (ScenarioGroupBaseRow base : bases) {
            GroupDimensionTemplate template = ScenarioDimensionMapper.resolve(base.apiVersion(), base.dimensionDefinitions(), templates, base.eventTypeL2Id());
            List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());

            List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
            List<ScenarioBaseRow> groupScenarios = scenariosByGroup.getOrDefault(base.scenarioGroupId(), List.of());
            Map<Long, Integer> adjusted = ScenarioProbabilityAllocator.redistributeRows(groupScenarios);
            List<ScenarioRowRes> scenarios = groupScenarios.stream()
                .map(s -> ScenarioRowRes.of(s, template.dimensions(selectedKeys, s.dimensionValues()), adjusted.get(s.scenarioId())))
                .toList();

            itemsByGroupId.put(base.scenarioGroupId(), ScenarioGroupListItemRes.of(base, dimensionColumns, scenarios));
        }

        return new ScenarioGroupListRes(
            ScenarioGroupAppliedFilterRes.from(request),
            PaginationRes.toRes(page.map(b -> itemsByGroupId.get(b.scenarioGroupId())))
        );
    }

    /**
     * 내가 만든 그룹 필터의 소유자 조건
     */
    private Long resolveMineUserId(ScenarioGroupListReq request) {
        return Boolean.TRUE.equals(request.mineOnly()) ? authorizationService.getUserNo().longValue() : null;
    }

    /**
     * [개별 모드 목록]
     * - 최신순 정렬/기간 필터 모두 시나리오 생성일시(create_dt) 기준
     * - adjustedProbability는 그룹 단위 계산이라, 페이지에 걸린 그룹들의 시나리오 전체를 일괄 조회해 재분배
     */
    @Override
    public ScenarioIndividualListRes getScenarioGroupScenarios(ScenarioGroupListReq request) {

        Page<ScenarioIndividualBaseRow> page = scenarioGroupRepository.findScenarioPage(request, resolveMineUserId(request));
        List<ScenarioIndividualBaseRow> bases = page.getContent();

        List<Long> eventTypeL2Ids = bases.stream()
            .map(ScenarioIndividualBaseRow::eventTypeL2Id)
            .distinct()
            .toList();

        Map<Long, GroupDimensionTemplate> templates = ScenarioDimensionMapper.parseTemplates(
            scenarioGroupRepository.findTemplatesByEventTypeL2Ids(eventTypeL2Ids)
        );

        // 재분배 확률은 그룹 단위 계산이라 페이지 내 그룹들의 시나리오 전체를 일괄 조회
        List<Long> groupIds = bases.stream().map(ScenarioIndividualBaseRow::scenarioGroupId).distinct().toList();

        Map<Long, Integer> adjusted = new LinkedHashMap<>();
        scenarioGroupRepository.findScenariosByGroupIds(groupIds).stream()
            .collect(Collectors.groupingBy(ScenarioBaseRow::scenarioGroupId, LinkedHashMap::new, Collectors.toList()))
            .values()
            .forEach(rows -> adjusted.putAll(ScenarioProbabilityAllocator.redistributeRows(rows)));

        Map<Long, ScenarioIndividualItemRes> itemsByScenarioId = new LinkedHashMap<>();
        for (ScenarioIndividualBaseRow base : bases) {
            GroupDimensionTemplate template = ScenarioDimensionMapper.resolve(base.apiVersion(), base.dimensionDefinitions(), templates, base.eventTypeL2Id());
            List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());
            List<ScenarioDimensionRes> dimensions = template.dimensions(selectedKeys, base.dimensionValues());

            itemsByScenarioId.put(base.scenarioId(),
                ScenarioIndividualItemRes.of(base, dimensions, adjusted.get(base.scenarioId())));
        }

        return new ScenarioIndividualListRes(
            ScenarioGroupAppliedFilterRes.from(request),
            PaginationRes.toRes(page.map(b -> itemsByScenarioId.get(b.scenarioId())))
        );
    }

    /**
     * [가격전망 그래프]
     * - MI Base 기준선: generated marker로 지정된 current MI Base의 latest completed run 가격
     * - 시나리오 기준선 : '케이스 마지막 실행이 모두 완료'된 최신 케이스의 실행 가격 (재실행 실패/진행 중이면 직전 완료 케이스 유지)
     * - 완료 케이스가 없는 시나리오는 executed=false, 값 null
     * - 판정 상태(실현/폐기)와 무관하게 그룹 내 모든 시나리오를 시리즈로 내린다
     * - x축은 전 시나리오 기간키 union, 시리즈별 빠진 구간은 null 패딩
     */
    @Override
    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit) {

        requireActiveGroup(scenarioGroupId);

        List<ScenarioBaseRow> scenarios = scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId);
        List<Long> scenarioIds = scenarioIdsOf(scenarios);

        // 시나리오 실행상태 조회
        ScenarioRunStates runStates = resolveRunStates(scenarioIds);

        return buildGraph(scenarioGroupId, indexName, periodUnit, scenarios, runStates).graph();
    }

    /**
     * [그래프 실행상태 폴링]
     * - 가격 없이 시나리오별 실행중(modelRunning)/준비중(preparing)만 가볍게 반환
     * - preparing은 실행 접수 후 RUNNING 케이스 생성 전 구간(파라미터 확정 반영, 엑셀 병합, 파일 스테이징) — 일괄 실행은 순차 접수라 아직 차례가 오지 않은 시나리오는 false일 수 있다
     * - FE는 anyRunning과 anyPreparing이 모두 false로 전환될 때 그래프를 재조회
     */
    @Override
    public ScenarioGroupRunStatusRes getScenarioGroupGraphRunStatus(Long scenarioGroupId) {

        // 시나리오 그룹 유효성 검증
        requireActiveGroup(scenarioGroupId);

        List<ScenarioBaseRow> scenarioRows = scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId);

        List<Long> scenarioIds = scenarioIdsOf(scenarioRows);
        ScenarioRunStates runStates = resolveRunStates(scenarioIds);
        Set<Long> preparingScenarioIds = findPreparingScenarioIds(scenarioIds);

        List<ScenarioRunStatus> scenarios = scenarioIds.stream()
            .map(scenarioId -> new ScenarioRunStatus(scenarioId, runStates.isRunning(scenarioId), preparingScenarioIds.contains(scenarioId)))
            .toList();

        return ScenarioGroupRunStatusRes.of(scenarioGroupId, scenarios);
    }

    /**
     * 실행 접수 후 준비중인 시나리오 집합.
     * 활성시나리오ID(actv_scnr_id) unique helper 컬럼 기준 조회라 시나리오당 최대 1행만 읽는다.
     */
    private Set<Long> findPreparingScenarioIds(List<Long> scenarioIds) {
        if (scenarioIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(modelExecutionPreparationRepository.findActiveScenarioIdsByStatuses(scenarioIds, MODEL_EXECUTION_PREPARING_STATUSES));
    }

    /**
     * [그래프 빌드 - 모든 인덱스]
     * - 엑셀 다운로드 등 전 인덱스(JKM/TTF/HH)를 한 번에 빌드해야 하는 곳에서 사용 (인덱스별 그래프, 단일 기간단위)
     * - 엑셀 변환/외부 기준선(CME) 병합은 호출 측 책임 (CME는 전용 DB라 dedicated 서비스에서만 접근)
     */
    public Map<IndexName, IndexGraph> buildGraphs(Long scenarioGroupId, GraphPeriodUnit periodUnit) {

        requireActiveGroup(scenarioGroupId);

        List<ScenarioBaseRow> scenarios = scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId);

        List<Long> scenarioIds = scenarioIdsOf(scenarios);

        // 시나리오 실행상태 조회
        ScenarioRunStates runStates = resolveRunStates(scenarioIds);

        Map<IndexName, IndexGraph> graphs = new LinkedHashMap<>();

        // JKM, TTF, HH 인덱스 전체
        for (IndexName indexName : IndexName.values()) {
            graphs.put(indexName, buildGraph(scenarioGroupId, indexName, periodUnit, scenarios, runStates));
        }

        return graphs;
    }

    private IndexGraph buildGraph(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit, List<ScenarioBaseRow> scenarios, ScenarioRunStates runStates) {

        List<Long> scenarioIds = scenarioIdsOf(scenarios);

        // 시나리오 목록의 기준선 : 가장 최신 COMPLETED 상태 케이스의 가격
        List<ScenarioPriceRow> scenariosPrices = scenarioCaseRepository.findLatestCompletedPricesByScenarioIds(scenarioIds, indexName, periodUnit.getDbUnit());

        // MI Base 기준선: generated marker로 지정된 current MI Base의 가격 결과
        List<MiBasePriceRow> miBasePrices = scenarioCaseRepository.findCurrentMiBasePrices(indexName, periodUnit.getDbUnit());

        ScenarioGroupGraphRes graph = ScenarioGroupGraphRes.of(scenarioGroupId, indexName, periodUnit, scenarios, miBasePrices, scenariosPrices, runStates.running(), runStates.completed());

        // 차트 표시 구간으로 클리핑 (CME/MI/시나리오 동일 창 — 화면 및 엑셀 공통 정책)
        ChartWindow window = ChartWindow.of(periodUnit, getMiBaseEndResult(miBasePrices, periodUnit));
        return new IndexGraph(graph.clip(window), window);
    }

    /**
     * MI Base 가장 큰 기간 조회
     */
    private static String getMiBaseEndResult(List<MiBasePriceRow> miBasePrices, GraphPeriodUnit periodUnit) {
        return miBasePrices.stream()
            .map(MiBasePriceRow::periodKey)
            .max(Comparator.naturalOrder())
            .map(periodUnit::formatPeriodKey)
            .orElse(null);
    }

    private static List<Long> scenarioIdsOf(List<ScenarioBaseRow> scenarios) {
        return scenarios.stream().map(ScenarioBaseRow::scenarioId).toList();
    }

    /**
     * 그룹에 포함된 시나리오들의 실행 상태를 running/completed 집합으로 도출
     * */
    private ScenarioRunStates resolveRunStates(List<Long> scenarioIds) {
        return ScenarioRunStates.from(scenarioCaseRepository.findScenarioRunStates(scenarioIds));
    }

    /**
     * 그룹 존재 검증
     *  - "없는 그룹"(404)과 "시나리오 없는 그룹"(빈 그래프)을 구분하기 위한 경량 exists 체크
     */
    private void requireActiveGroup(Long scenarioGroupId) {
        if (!scenarioGroupRepository.existsByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId));
        }
    }

    /**
     * [그룹 상세]
     * - status는 DB 상태를 화면 판정 상태로 매핑 (creating/created -> pending)
     * - adjustedProbability는 목록과 동일한 조회 시점 계산값(비영속)
     * - eventBundles는 구성 이벤트 전체(표시 순서), evidence는 최신 성공 create operation의 영속 스냅샷에서 추출 (설계 외 화면은 DB 원천)
     */
    @Override
    public ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId) {
        ScenarioGroupDetailBaseRow base = scenarioGroupRepository.findScenarioGroupDetail(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId)));

        List<EventBundle> eventBundles = ScenarioDesignAgentMapper.toEventBundles(
            scenarioGroupRepository.findBaseEventsByScenarioGroupId(scenarioGroupId));

        // 근거 보기
        DesignEvidence evidence = null;
        if (base.apiVersion() == ScenarioApiVersion.V4) {
            try {
                evidence = ScenarioDesignAgentMapper.toEvidence(scenarioDesignAgentApi.getPresentation(scenarioGroupId, null));
            } catch (Exception e) {
                log.warn("[ScenarioGroup] 근거 조회용 Agent presentation 호출 실패 - evidence 생략: scenarioGroupId={}, cause={}", scenarioGroupId, e.getMessage());
            }
        }

        // eventTypeL2Id는 left join 산출이라 매핑 소실 그룹에서 null일 수 있다 — v1 template 폴백만 생략 (v4 definitions 경로는 무관)
        GroupDimensionTemplate template = ScenarioDimensionMapper.resolve(
            base.apiVersion(),
            base.dimensionDefinitions(),
            base.eventTypeL2Id() == null
                ? Map.of()
                : ScenarioDimensionMapper.parseTemplates(scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id()))),
            base.eventTypeL2Id());

        List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());

        List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
        List<ScenarioBaseRow> groupScenarios = scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId);
        Map<Long, Integer> adjusted = ScenarioProbabilityAllocator.redistributeRows(groupScenarios);
        List<ScenarioGroupDetailRowRes> scenarios = groupScenarios.stream()
            .map(s -> ScenarioGroupDetailRowRes.of(
                s,
                template.dimensions(selectedKeys, s.dimensionValues()),
                ScenarioDisplayStatus.from(s.status()),
                adjusted.get(s.scenarioId())
            ))
            .toList();

        // 시나리오별 가격전망 해석 section (이미 로드한 시나리오 재사용 — 재조회 없이 조립)
        PriceInterpretationSectionRes priceInterpretation = priceInterpretationStore.buildSection(scenarioGroupId, groupScenarios);

        return ScenarioGroupDetailRes.of(base, eventBundles, evidence, dimensionColumns, scenarios, priceInterpretation);
    }

    /**
     * [개별 시나리오 상세] (내러티브 + 주요 가정 + Signpost)
     * - signpost 모니터링은 발생일시(occurred_at) 오름차순, 미발생(null)은 맨 뒤
     * - adjustedProbability는 소속 그룹 전체 시나리오 기준으로 재분배한 값
     */
    @Override
    public ScenarioDetailViewRes getScenarioDetail(Long scenarioGroupId, Long scenarioId) {
        ScenarioDetailBaseRow base = scenarioGroupRepository.findScenarioDetail(scenarioGroupId, scenarioId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario not found: scenarioGroupId=%d, scenarioId=%d".formatted(scenarioGroupId, scenarioId)));

        // eventTypeL2Id는 left join 산출이라 매핑 소실 그룹에서 null일 수 있다 — v1 template 폴백만 생략 (v4 definitions 경로는 무관)
        GroupDimensionTemplate template = ScenarioDimensionMapper.resolve(
            base.apiVersion(),
            base.dimensionDefinitions(),
            base.eventTypeL2Id() == null
                ? Map.of()
                : ScenarioDimensionMapper.parseTemplates(scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id()))),
            base.eventTypeL2Id());

        List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());

        List<ScenarioDimensionRes> dimensions = template.dimensions(selectedKeys, base.dimensionValues());

        List<ScenarioSignpostDefinitionViewRes> signpostDefinitions = scenarioGroupRepository.findSignpostsByScenarioId(scenarioId).stream()
                .map(ScenarioSignpostDefinitionViewRes::of)
                .toList();

        List<ScenarioSignpostMonitoringRes> signpostMonitorings = scenarioGroupRepository.findSignpostEventsByScenarioId(scenarioId).stream()
                .map(ScenarioSignpostMonitoringRes::of)
                .toList();

        Map<Long, Integer> adjusted = ScenarioProbabilityAllocator.redistributeRows(scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId));

        return ScenarioDetailViewRes.of(base, dimensions, adjusted.get(scenarioId), signpostDefinitions, signpostMonitorings);
    }

    /**
     * [시나리오 판정 상태 변경]
     * - 실현 시 같은 그룹의 나머지 시나리오는 모두 폐기로 캐스케이드 (그룹 내 실현은 항상 1개), 함께 폐기된 ID는 응답 discardedScenarioIds로 전달
     * - 그룹 status는 매 변경마다 재도출: 실현 시나리오가 있으면 realized, 없으면 created 환원
     * - 폐기되는 시나리오(직접 폐기 + 실현 캐스케이드 폐기)는 연계된 수급모델 실행도 함께 soft delete (실행 중 건은 실패 처리 후 삭제)
     */
    @Override
    @Transactional
    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioDisplayStatus status) {

        // 설계 진행 중(creating) 그룹 가드 — deriveStatus가 creating을 created로 덮어써 진행 중인 설계 세션을
        // 되돌릴 수 없게 오염시키므로, 실현 판정은 설계가 끝난 그룹에서만 허용한다
        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId)));

        if (group.getStatus() == ScenarioGroupStatus.CREATING) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "설계 진행 중인 그룹은 시나리오 상태를 변경할 수 없습니다: %d".formatted(scenarioGroupId));
        }

        // 실현 캐스케이드(나머지 전체 폐기)가 동시 요청과 어긋나지 않도록 그룹 시나리오 전체를 잠그고 시작
        List<MiScenario> scenarios = scenarioGroupRepository.findScenarioEntitiesByGroupIdForUpdate(scenarioGroupId);

        MiScenario target = scenarios.stream()
            .filter(s -> s.getScenarioId().equals(scenarioId))
            .findFirst()
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario not found: scenarioGroupId=%d, scenarioId=%d".formatted(scenarioGroupId, scenarioId)));

        target.updateStatus(status);

        // 기획: 그룹 내 시나리오가 하나라도 '실현'되면 나머지는 모두 '폐기' 처리
        List<Long> discardedScenarioIds = List.of();
        if (status == ScenarioDisplayStatus.REALIZED) {
            List<MiScenario> others = scenarios.stream()
                .filter(s -> !s.getScenarioId().equals(scenarioId))
                .toList();
            others.forEach(s -> s.updateStatus(ScenarioDisplayStatus.DISCARDED));
            discardedScenarioIds = others.stream().map(MiScenario::getScenarioId).toList();
        }

        // 그룹 status 재도출: '그룹 realized = 실현 시나리오 존재' 불변식을 실현/되돌림/폐기 모든 변경 경로에서 유지한다.
        boolean anyRealized = scenarios.stream().anyMatch(s -> s.getStatus() == ScenarioStatus.REALIZED);
        target.getScenarioGroup().deriveStatus(anyRealized);

        //  폐기되는 시나리오의 수급모델 실행을 함께 soft delete
        List<Long> executionDeleteTargetIds = status == ScenarioDisplayStatus.DISCARDED ? List.of(scenarioId) : discardedScenarioIds;
        softDeleteScenarioExecutions(executionDeleteTargetIds);

        return ScenarioStatusRes.of(scenarioId, status, discardedScenarioIds);
    }

    /**
     * [그룹 삭제]
     * - 그룹 + 하위 시나리오 + 하위 signpost + 이벤트 매핑을 함께 del_yn 처리, 이후 모든 조회에서 제외
     * - signpost는 시나리오/그룹을 지우기 전에 먼저 조회해 soft-delete (삭제 후엔 join에서 빠지므로)
     * - 이벤트 매핑을 함께 지우는 이유는 매핑만 살아남으면 그룹 삭제 여부를 확인하지 않는 조회가 유령 연결을 보기 때문이다.
     *   현재 조회 경로는 모두 그룹 del_yn을 함께 보므로 새는 곳은 없지만, 매핑 자체를 정리해 그 전제에 의존하지 않게 한다.
     * - 멀티 이벤트 전환으로 이벤트:활성 그룹은 N:M이라, 삭제해도 같은 이벤트에 남은 다른 활성 그룹이 있으면 설계 상태는 유지된다.
     *   해당 이벤트 조합의 활성 그룹이 모두 사라져야 다시 설계 가능해진다.
     */
    @Override
    @Transactional
    public void deleteScenarioGroup(Long scenarioGroupId) {

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId)));

        priceInterpretationStore.softDeleteByGroup(scenarioGroupId);
        List<MiScenario> scenarios = scenarioGroupRepository.findScenarioEntitiesByGroupIdForUpdate(scenarioGroupId);
        List<MiSignpost> signposts = scenarioGroupRepository.findSignpostEntitiesByGroupId(scenarioGroupId);
        List<MiScenarioGroupEvent> groupEvents = scenarioGroupEventRepository.findByScenarioGroup_ScenarioGroupIdAndIsDeletedFalse(scenarioGroupId);
        signposts.forEach(MiSignpost::softDelete);
        scenarios.forEach(MiScenario::softDelete);
        groupEvents.forEach(MiScenarioGroupEvent::softDelete);
        group.softDelete();
        scenarioGroupRepository.flush();

        softDeleteScenarioExecutions(scenarios.stream().map(MiScenario::getScenarioId).toList());

    }

    private void softDeleteScenarioExecutions(List<Long> scenarioIds) {
        if (scenarioIds.isEmpty()) {
            return;
        }

        LocalDateTime modifiedAt = LocalDateTime.now();
        String actorLoginId = authorizationService.getSubject();
        List<Long> caseIds = scenarioCaseRepository.findCaseIdsByScenarioIds(scenarioIds);

        if (!caseIds.isEmpty()) {
            scenarioCaseRunHistoryRepository.cancelRunningByCaseIds(
                caseIds,
                FundamentalModelRunStatus.FAILED,
                FundamentalModelRunStatus.RUNNING,
                modifiedAt,
                SCENARIO_CANCELLED,
                modifiedAt,
                actorLoginId
            );
        }
        scenarioCaseRepository.cancelRunningByScenarioIds(
            scenarioIds,
            FundamentalModelCaseStatus.FAILED,
            FundamentalModelCaseStatus.RUNNING,
            modifiedAt,
            actorLoginId
        );
        scenarioCaseRepository.softDeleteByScenarioIds(scenarioIds, modifiedAt, actorLoginId);
        if (!caseIds.isEmpty()) {
            scenarioCaseRunHistoryRepository.softDeleteByCaseIds(caseIds, modifiedAt, actorLoginId);
        }
    }

}

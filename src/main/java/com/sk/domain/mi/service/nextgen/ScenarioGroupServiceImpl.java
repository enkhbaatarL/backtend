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
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationStore;
import com.sk.domain.mi.service.nextgen.support.IndexGraph;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.mi.service.nextgen.support.ScenarioRunStates;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.entity.master.tmm.MiSignpost;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.projection.MiBasePriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioIndividualBaseRow;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final PriceInterpretationStore priceInterpretationStore;

    /**
     * [그룹 모드 목록]
     * - 최신순 정렬/기간 필터 모두 그룹 생성일시(create_dt) 기준
     * - adjustedProbability는 조회 시점 계산값(비영속) - 폐기 제외 비례 재분배로 활성 시나리오 합 100, 폐기는 0
     */
    @Override
    public ScenarioGroupListRes getScenarioGroups(ScenarioGroupListReq request) {

        Page<ScenarioGroupBaseRow> page = scenarioGroupRepository.findScenarioGroupPage(request);
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
            GroupDimensionTemplate template = templates.getOrDefault(base.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);
            List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(base.dimensionSelected());

            List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
            List<ScenarioBaseRow> groupScenarios = scenariosByGroup.getOrDefault(base.scenarioGroupId(), List.of());
            Map<Long, Integer> adjusted = ScenarioProbabilityAllocator.redistributeRows(groupScenarios);
            List<ScenarioRowRes> scenarios = groupScenarios.stream()
                .map(s -> ScenarioRowRes.of(s, template.dimensions(selectedKeys, s.dimensionValues()),
                    adjusted.get(s.scenarioId())))
                .toList();

            itemsByGroupId.put(base.scenarioGroupId(), ScenarioGroupListItemRes.of(base, dimensionColumns, scenarios));
        }

        return new ScenarioGroupListRes(
            ScenarioGroupAppliedFilterRes.from(request),
            PaginationRes.toRes(page.map(b -> itemsByGroupId.get(b.scenarioGroupId())))
        );
    }

    /**
     * [개별 모드 목록]
     * - 최신순 정렬/기간 필터 모두 시나리오 생성일시(create_dt) 기준
     * - adjustedProbability는 그룹 단위 계산이라, 페이지에 걸린 그룹들의 시나리오 전체를 일괄 조회해 재분배
     */
    @Override
    public ScenarioIndividualListRes getScenarioGroupScenarios(ScenarioGroupListReq request) {

        Page<ScenarioIndividualBaseRow> page = scenarioGroupRepository.findScenarioPage(request);
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
            GroupDimensionTemplate template = templates.getOrDefault(base.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);
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
     * - 시나리오 라인 = '케이스 마지막 실행이 모두 완료'된 최신 케이스의 실행 가격 (재실행 실패/진행 중이면 직전 완료 케이스 유지)
     * - 완료 케이스가 없는 시나리오는 executed=false, 값 null
     * - 판정 상태(실현/폐기)와 무관하게 그룹 내 모든 시나리오를 시리즈로 내린다
     * - x축은 전 시나리오 기간키 union, 시리즈별 빠진 구간은 null 패딩
     */
    @Override
    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit) {

        // 시나리오 그룹 유효성 검증
        requireActiveGroup(scenarioGroupId);

        List<ScenarioBaseRow> scenarios = scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId);
        List<Long> scenarioIds = scenarioIdsOf(scenarios);

        // 시나리오 실행상태 조회
        ScenarioRunStates runStates = resolveRunStates(scenarioIds);

        return buildGraph(scenarioGroupId, indexName, periodUnit, scenarios, runStates).graph();
    }

    /**
     * [그래프 실행상태 폴링]
     * - 가격 없이 시나리오별 실행중(modelRunning)만 가볍게 반환 — FE는 anyRunning(=modelRunning) true→false 전환 시 그래프를 재조회
     */
    @Override
    public ScenarioGroupRunStatusRes getScenarioGroupGraphRunStatus(Long scenarioGroupId) {

        // 시나리오 그룹 유효성 검증
        requireActiveGroup(scenarioGroupId);
        List<Long> scenarioIds = scenarioIdsOf(scenarioGroupRepository.findScenariosByGroupId(scenarioGroupId));
        ScenarioRunStates runStates = resolveRunStates(scenarioIds);

        List<ScenarioRunStatus> scenarios = scenarioIds.stream()
            .map(scenarioId -> new ScenarioRunStatus(scenarioId, runStates.isRunning(scenarioId)))
            .toList();

        return ScenarioGroupRunStatusRes.of(scenarioGroupId, scenarios);
    }

    /**
     * [그래프 빌드 - 모든 인덱스]
     * - 엑셀 다운로드 등 전 인덱스(JKM/TTF/HH)를 한 번에 빌드해야 하는 곳에서 사용 (인덱스별 그래프, 단일 기간단위)
     * - 엑셀 변환/외부 기준선(CME) 병합은 호출 측 책임 (CME는 전용 DB라 dedicated 서비스에서만 접근)
     */
    public Map<IndexName, IndexGraph> buildGraphs(Long scenarioGroupId, GraphPeriodUnit periodUnit) {

        // 시나리오 그룹 유효성 검증
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

        // 가장 최신 COMPLETED 상태 케이스의 가격
        List<ScenarioPriceRow> prices = scenarioCaseRepository.findLatestCompletedPricesByScenarioIds(scenarioIds, indexName, periodUnit.getDbUnit());

        // MI Base 기준선 = 실행유형코드(MI_BASE) 최신 케이스의 가격 결과
        List<MiBasePriceRow> miBasePrices = scenarioCaseRepository.findLatestMiBasePrices(indexName, periodUnit.getDbUnit());

        ScenarioGroupGraphRes graph = ScenarioGroupGraphRes.of(scenarioGroupId, indexName, periodUnit, scenarios, prices, runStates.running(), runStates.completed(), miBasePrices);

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
     */
    @Override
    public ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId) {
        ScenarioGroupDetailBaseRow base = scenarioGroupRepository.findScenarioGroupDetail(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId)));

        GroupDimensionTemplate template = ScenarioDimensionMapper.parseTemplates(
                scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id())))
            .getOrDefault(base.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);
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

        return ScenarioGroupDetailRes.of(base, dimensionColumns, scenarios, priceInterpretation);
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

        GroupDimensionTemplate template = ScenarioDimensionMapper.parseTemplates(scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id())))
            .getOrDefault(base.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);

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
     */
    @Override
    @Transactional
    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioDisplayStatus status) {

        List<MiScenario> scenarios = scenarioGroupRepository.findScenarioEntitiesByGroupId(scenarioGroupId);

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

        return ScenarioStatusRes.of(scenarioId, status, discardedScenarioIds);
    }

    /**
     * [그룹 삭제]
     * - 그룹 + 하위 시나리오 + 하위 signpost + 시나리오별 가격전망 해석을 함께 del_yn 처리, 이후 모든 조회에서 제외
     * - signpost/가격전망해석은 시나리오/그룹을 지우기 전에 먼저 조회해 soft-delete (삭제 후엔 join에서 빠지므로)
     * - 이벤트:활성 그룹 = 1:1이므로 삭제 시 해당 이벤트의 scenarioDesignStatus는 designable(재설계 가능)로 복귀
     */
    @Override
    @Transactional
    public void deleteScenarioGroup(Long scenarioGroupId) {

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId)));

        priceInterpretationStore.softDeleteByGroup(scenarioGroupId);
        scenarioGroupRepository.findSignpostEntitiesByGroupId(scenarioGroupId).forEach(MiSignpost::softDelete);
        scenarioGroupRepository.findScenarioEntitiesByGroupId(scenarioGroupId).forEach(MiScenario::softDelete);
        group.softDelete();

    }

}

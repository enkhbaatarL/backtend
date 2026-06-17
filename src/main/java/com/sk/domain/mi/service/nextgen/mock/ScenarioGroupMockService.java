package com.sk.domain.mi.service.nextgen.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupAppliedFilterRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.mi.service.nextgen.support.ScenarioGroupGraphExcelMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.enums.nextgen.SortOrder;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioGroupMockService {

    private final ObjectMapper objectMapper;

    private List<ScenarioGroupListItemRes> groups;
    private Map<Long, List<ScenarioGroupGraphRes>> graphMap;
    private Map<Long, ScenarioGroupDetailRes> groupDetailMap;
    private Map<Long, ScenarioDetailViewRes> scenarioDetailMap;

    /** [mock 전용] 상태 변경 API가 바꾼 시나리오 상태 (scenarioId → status). reset API로 초기화. */
    private final Map<Long, ScenarioDisplayStatus> statusOverrides = new ConcurrentHashMap<>();

    /** [mock 전용] soft delete 된 시나리오 그룹 ID. 그룹 삭제 시 그룹 + 하위 시나리오가 조회에서 제외된다. reset API로 초기화. */
    private final Set<Long> deletedGroupIds = ConcurrentHashMap.newKeySet();

    /** [mock 전용] 수급모델 실행중 시나리오 ID. 모델실행 mock(submit→RUNNING/완료→idle)이 갱신, 그래프 modelRunning 표기. */
    private final Set<Long> runningScenarioIds = ConcurrentHashMap.newKeySet();

    /** [mock 전용] 수급모델 실행중 표기 (모델실행 mock에서 호출). */
    public void markModelRunning(Long scenarioId) {
        if (scenarioId != null) {
            runningScenarioIds.add(scenarioId);
        }
    }

    /** [mock 전용] 수급모델 실행중 해제 (모델실행 mock에서 호출). */
    public void markModelIdle(Long scenarioId) {
        if (scenarioId != null) {
            runningScenarioIds.remove(scenarioId);
        }
    }

    @PostConstruct
    void init() {
        try {
            reload();
            log.info("[Mock] Scenario group mock data loaded: groups={}, graphs={}, groupDetails={}, scenarioDetails={}",
                groups.size(), graphMap.size(), groupDetailMap.size(), scenarioDetailMap.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load scenario group mock data", e);
            groups = Collections.emptyList();
            graphMap = Collections.emptyMap();
            groupDetailMap = Collections.emptyMap();
            scenarioDetailMap = Collections.emptyMap();
        }
    }

    /** fixture 초기 데이터셋을 재적재한다 (init + reset 공용). 설계 확정으로 추가된 그룹 등 메모리 변경분이 모두 제거된다. */
    private void reload() throws IOException {
        loadGroups();
        loadGraphs();
        loadGroupDetails();
        loadScenarioDetails();
    }

    private void loadGroupDetails() throws IOException {
        try (InputStream is = new ClassPathResource("mock/scenarios/scenario-group-detail.json").getInputStream()) {
            groupDetailMap = objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    private void loadScenarioDetails() throws IOException {
        try (InputStream is = new ClassPathResource("mock/scenarios/scenario-individual-detail.json").getInputStream()) {
            scenarioDetailMap = objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    private void loadGroups() throws IOException {
        try (InputStream is = new ClassPathResource("mock/scenarios/scenario-group-list.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            groups = objectMapper.treeToValue(
                root.get("scenarioGroups"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ScenarioGroupListItemRes.class)
            );
        }
    }

    private void loadGraphs() throws IOException {
        try (InputStream is = new ClassPathResource("mock/scenarios/scenario-group-graph.json").getInputStream()) {
            graphMap = objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    public ScenarioGroupListRes getScenarioGroups(ScenarioGroupListReq request) {
        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        Comparator<ScenarioGroupListItemRes> comparator = Comparator.comparing(ScenarioGroupListItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<ScenarioGroupListItemRes> filtered = groups.stream()
            .filter(g -> !deletedGroupIds.contains(g.scenarioGroupId()))
            .filter(g -> startDateTime == null || !g.updatedAt().isBefore(startDateTime))
            .filter(g -> endDateTime == null || g.updatedAt().isBefore(endDateTime))
            .filter(g -> request.keyword().isEmpty() || g.groupName().contains(request.keyword()))
            .sorted(comparator)
            // 임베드된 시나리오 표에도 상태변경/폐기 오버레이 + 재분배 확률 반영 (상세·그래프와 일관)
            .map(this::overlayGroupScenarios)
            .toList();

        return new ScenarioGroupListRes(ScenarioGroupAppliedFilterRes.from(request), paginate(filtered, request));
    }

    public ScenarioIndividualListRes getScenarioGroupScenarios(ScenarioGroupListReq request) {
        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        List<ScenarioIndividualItemRes> all = new ArrayList<>();
        for (ScenarioGroupListItemRes group : groups) {
            // 그룹 삭제 시 그룹 내 모든 시나리오도 제외
            if (group.scenarios() == null || deletedGroupIds.contains(group.scenarioGroupId())) {
                continue;
            }
            // 개별 행도 상태변경/폐기 오버레이된 시나리오에서 파생 (그룹 목록과 일관)
            ScenarioGroupListItemRes overlaid = overlayGroupScenarios(group);
            for (ScenarioRowRes row : overlaid.scenarios()) {
                all.add(ScenarioIndividualItemRes.of(overlaid, row));
            }
        }

        Comparator<ScenarioIndividualItemRes> comparator =
            Comparator.comparing(ScenarioIndividualItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<ScenarioIndividualItemRes> filtered = all.stream()
            .filter(s -> startDateTime == null || !s.updatedAt().isBefore(startDateTime))
            .filter(s -> endDateTime == null || s.updatedAt().isBefore(endDateTime))
            .filter(s -> request.keyword().isEmpty() || s.scenarioName().contains(request.keyword()))
            .sorted(comparator)
            .toList();

        return new ScenarioIndividualListRes(ScenarioGroupAppliedFilterRes.from(request), paginate(filtered, request));
    }

    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit) {
        if (deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: " + scenarioGroupId);
        }
        List<ScenarioGroupGraphRes> graphs = graphMap.get(scenarioGroupId);
        if (graphs == null) {
            // 설계 확정 등으로 생성됐으나 그래프 fixture 미보유(수급모델 미실행) → 빈 그래프 반환(404 방지)
            if (groupDetailMap.containsKey(scenarioGroupId)) {
                return emptyGraph(scenarioGroupId, indexName, periodUnit);
            }
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: " + scenarioGroupId);
        }
        ScenarioGroupGraphRes graph = graphs.stream()
            .filter(g -> indexName.getCode().equalsIgnoreCase(g.indexName()))
            .filter(g -> periodUnit == g.periodUnit())
            .findFirst()
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: scenarioGroupId=" + scenarioGroupId + ", indexName=" + indexName));
        return applyScenarioStatusToGraph(scenarioGroupId, graph);
    }

    /** 그래프 시리즈에 시나리오 판정 상태를 오버레이 (그룹 상세의 effectiveStatus = 상태변경/폐기 반영, list/detail과 일관). */
    private ScenarioGroupGraphRes applyScenarioStatusToGraph(Long scenarioGroupId, ScenarioGroupGraphRes graph) {
        ScenarioGroupDetailRes detail = groupDetailMap.get(scenarioGroupId);
        Map<Long, ScenarioDisplayStatus> statusByScenario = detail == null ? Map.of()
            : detail.scenarios().stream()
                .collect(Collectors.toMap(ScenarioGroupDetailRowRes::scenarioId, this::effectiveStatus));

        List<ScenarioGroupGraphRes.ScenarioSeries> series = graph.scenarioSeries().stream()
            .map(s -> {
                ScenarioDisplayStatus status = statusByScenario.getOrDefault(s.scenarioId(), ScenarioDisplayStatus.PENDING);
                return new ScenarioGroupGraphRes.ScenarioSeries(
                    s.scenarioId(), s.scenarioName(), s.baseCase(), s.executed(),
                    runningScenarioIds.contains(s.scenarioId()), status, status.getLabel(),
                    s.baseCaseValues(), s.userAdjustedValues());
            })
            .toList();

        return new ScenarioGroupGraphRes(
            graph.scenarioGroupId(), graph.indexName(), graph.periodUnit(), graph.periodUnitLabel(),
            graph.currency(), graph.unit(), graph.dates(), graph.baseSeries(), series);
    }

    /** 그래프 fixture가 없는 그룹(설계 확정 직후 등)용 빈 그래프 — 시나리오는 미실행 시리즈로만 노출. */
    private ScenarioGroupGraphRes emptyGraph(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit) {
        ScenarioGroupDetailRes detail = groupDetailMap.get(scenarioGroupId);
        List<ScenarioGroupGraphRes.ScenarioSeries> series = detail == null ? List.of()
            : detail.scenarios().stream()
                .map(row -> {
                    ScenarioDisplayStatus status = effectiveStatus(row);
                    return new ScenarioGroupGraphRes.ScenarioSeries(
                        row.scenarioId(), row.scenarioName(), Boolean.TRUE.equals(row.baseCase()),
                        false, runningScenarioIds.contains(row.scenarioId()),
                        status, status.getLabel(), null, null);
                })
                .toList();
        return new ScenarioGroupGraphRes(
            scenarioGroupId, indexName.getCode(), periodUnit, periodUnit.getLabel(),
            "USD", "MMBtu", List.of(), List.of(), series);
    }

    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId, GraphPeriodUnit periodUnit) {
        if (deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: " + scenarioGroupId);
        }
        List<ScenarioGroupGraphRes> graphs = graphMap.get(scenarioGroupId);
        if (graphs == null) {
            // 설계 확정 등으로 생성됐으나 그래프 fixture 미보유(수급모델 미실행) → 빈 그래프로 내보냄(404 방지, 화면 그래프와 동일)
            if (!groupDetailMap.containsKey(scenarioGroupId)) {
                throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "Scenario group graph not found: " + scenarioGroupId);
            }
            List<ScenarioGroupGraphRes> empties = Arrays.stream(IndexName.values())
                .map(idx -> emptyGraph(scenarioGroupId, idx, periodUnit))
                .toList();
            return ScenarioGroupGraphExcelMapper.toExcel(empties);
        }
        // 화면 그래프와 동일하게 시나리오 판정 상태(상태변경/폐기)·실행중 표기를 오버레이해 내보낸다
        return ScenarioGroupGraphExcelMapper.toExcel(graphs.stream()
            .filter(g -> periodUnit == g.periodUnit())
            .map(g -> applyScenarioStatusToGraph(scenarioGroupId, g))
            .toList());
    }

    public ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId) {
        ScenarioGroupDetailRes detail = groupDetailMap.get(scenarioGroupId);
        if (detail == null || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group detail not found: " + scenarioGroupId);
        }
        return applyStatusOverrides(detail);
    }

    /** mock 메모리에 저장된 상태 변경분 반영 + 폐기 제외 재분배 확률 계산 */
    private ScenarioGroupDetailRes applyStatusOverrides(ScenarioGroupDetailRes detail) {
        Map<Long, Integer> adjusted = redistributeRows(detail.scenarios());
        List<ScenarioGroupDetailRowRes> rows = detail.scenarios().stream()
            .map(row -> {
                ScenarioDisplayStatus status = effectiveStatus(row);
                return new ScenarioGroupDetailRowRes(
                    row.scenarioId(), row.scenarioName(), row.baseCase(),
                    row.dimensions(), row.probability(), adjusted.get(row.scenarioId()),
                    status, status.getLabel());
            })
            .toList();
        return new ScenarioGroupDetailRes(
            detail.scenarioGroupId(), detail.groupName(), detail.createdAt(), detail.updatedAt(),
            detail.baseEvent(), detail.summary(), detail.scenarioCount(),
            detail.dimensionColumns(), rows);
    }

    private Map<Long, Integer> redistributeRows(List<ScenarioGroupDetailRowRes> rows) {
        return ScenarioProbabilityAllocator.redistribute(rows.stream()
            .map(row -> new ScenarioProbabilityAllocator.Item(
                row.scenarioId(),
                row.probability() == null ? null : BigDecimal.valueOf(row.probability()),
                effectiveStatus(row) == ScenarioDisplayStatus.DISCARDED))
            .toList());
    }

    private ScenarioDisplayStatus effectiveStatus(ScenarioGroupDetailRowRes row) {
        ScenarioDisplayStatus override = statusOverrides.get(row.scenarioId());
        if (override != null) {
            return override;
        }
        return row.status() == null ? ScenarioDisplayStatus.PENDING : row.status();
    }

    /** 그룹 목록/개별 목록의 임베드 시나리오 표에 상태변경/폐기 오버레이 + 재분배 확률을 입힌 새 그룹 아이템을 만든다. */
    private ScenarioGroupListItemRes overlayGroupScenarios(ScenarioGroupListItemRes group) {
        List<ScenarioRowRes> scenarios = group.scenarios();
        if (scenarios == null || scenarios.isEmpty()) {
            return group;
        }
        Map<Long, Integer> adjusted = redistributeRowRes(scenarios);
        List<ScenarioRowRes> rows = scenarios.stream()
            .map(row -> {
                ScenarioDisplayStatus status = effectiveStatus(row);
                return new ScenarioRowRes(
                    row.scenarioId(), row.scenarioName(), row.baseCase(),
                    row.dimensions(), row.probability(), adjusted.get(row.scenarioId()),
                    status, status.getLabel());
            })
            .toList();
        return new ScenarioGroupListItemRes(
            group.scenarioGroupId(), group.groupName(), group.createdAt(), group.updatedAt(),
            group.summary(), group.scenarioCount(), group.dimensionColumns(), rows);
    }

    private Map<Long, Integer> redistributeRowRes(List<ScenarioRowRes> rows) {
        return ScenarioProbabilityAllocator.redistribute(rows.stream()
            .map(row -> new ScenarioProbabilityAllocator.Item(
                row.scenarioId(),
                row.probability() == null ? null : BigDecimal.valueOf(row.probability()),
                effectiveStatus(row) == ScenarioDisplayStatus.DISCARDED))
            .toList());
    }

    private ScenarioDisplayStatus effectiveStatus(ScenarioRowRes row) {
        ScenarioDisplayStatus override = statusOverrides.get(row.scenarioId());
        if (override != null) {
            return override;
        }
        return row.status() == null ? ScenarioDisplayStatus.PENDING : row.status();
    }

    public Optional<String> findScenarioName(Long scenarioId) {
        ScenarioDetailViewRes detail = scenarioDetailMap.get(scenarioId);
        return Optional.ofNullable(detail).map(ScenarioDetailViewRes::scenarioName);
    }

    public ScenarioDetailViewRes getScenarioDetail(Long scenarioGroupId, Long scenarioId) {
        ScenarioDetailViewRes detail = scenarioDetailMap.get(scenarioId);
        if (detail == null
            || (detail.scenarioGroupId() != null && !detail.scenarioGroupId().equals(scenarioGroupId))
            || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario detail not found: scenarioGroupId=" + scenarioGroupId + ", scenarioId=" + scenarioId);
        }
        ScenarioGroupDetailRes groupDetail = groupDetailMap.get(scenarioGroupId);
        if (groupDetail == null) {
            return detail;
        }
        return detail.withAdjustedProbability(redistributeRows(groupDetail.scenarios()).get(scenarioId));
    }

    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioDisplayStatus status) {
        ScenarioGroupDetailRes detail = groupDetailMap.get(scenarioGroupId);
        boolean exists = detail != null
            && !deletedGroupIds.contains(scenarioGroupId)
            && detail.scenarios().stream().anyMatch(s -> s.scenarioId().equals(scenarioId));
        if (!exists) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario not found: scenarioGroupId=" + scenarioGroupId + ", scenarioId=" + scenarioId);
        }
        // mock: 변경분을 메모리에 저장 → 그룹 상세 조회에 반영, reset API로 초기화 가능
        statusOverrides.put(scenarioId, status);
        // 기획: 그룹 내 시나리오가 하나라도 '실현'되면 나머지는 모두 '폐기' 처리
        List<Long> discardedScenarioIds = List.of();
        if (status == ScenarioDisplayStatus.REALIZED) {
            discardedScenarioIds = detail.scenarios().stream()
                .map(ScenarioGroupDetailRowRes::scenarioId)
                .filter(id -> !id.equals(scenarioId))
                .toList();
            discardedScenarioIds.forEach(id -> statusOverrides.put(id, ScenarioDisplayStatus.DISCARDED));
        }
        return ScenarioStatusRes.of(scenarioId, status, discardedScenarioIds);
    }

    /**
     * [mock 전용] 그룹 + 하위 시나리오 soft delete. 예외를 던지지 않고 존재 여부를 반환한다.
     * <p>미확정 설계(그룹 미materialize, 세션만 존재) 취소도 같은 삭제 API로 처리되므로,
     * 그룹이 없으면 조용히 false를 반환하고 호출측(delegator)이 세션 제거 여부와 함께 404를 판단한다.
     * @return 실제로 삭제된 그룹이 있었으면 true
     */
    public boolean deleteScenarioGroupIfPresent(Long scenarioGroupId) {
        boolean exists = !deletedGroupIds.contains(scenarioGroupId)
            && groups.stream().anyMatch(g -> g.scenarioGroupId().equals(scenarioGroupId));
        if (exists) {
            deletedGroupIds.add(scenarioGroupId);
            // 삭제된 그룹 하위 시나리오의 실행중 표기도 함께 정리 (잔존 방지)
            clearRunningMarks(scenarioGroupId);
        }
        return exists;
    }

    /** 그룹 삭제 시 하위 시나리오의 수급모델 실행중 표기를 제거한다. */
    private void clearRunningMarks(Long scenarioGroupId) {
        ScenarioGroupDetailRes detail = groupDetailMap.get(scenarioGroupId);
        if (detail == null || detail.scenarios() == null) {
            return;
        }
        detail.scenarios().forEach(s -> runningScenarioIds.remove(s.scenarioId()));
    }

    /** [mock 전용] 그룹이 soft delete 됐는지 — 이벤트 설계가능 판정에서 사용(삭제된 그룹의 이벤트는 다시 설계가능). */
    public boolean isGroupDeleted(Long scenarioGroupId) {
        return scenarioGroupId != null && deletedGroupIds.contains(scenarioGroupId);
    }

    /** [mock 전용] 상태 변경분·그룹 삭제분·실행중 표기·설계 확정으로 추가된 그룹을 모두 초기 fixture로 되돌린다. */
    public void resetScenarioStatus() {
        statusOverrides.clear();
        deletedGroupIds.clear();
        runningScenarioIds.clear();
        try {
            reload(); // materialize된 그룹 등 메모리 변경분 제거 → fixture 초기값 복원
        } catch (IOException e) {
            log.error("[Mock] Failed to reload scenario group mock data on reset", e);
        }
    }

    /**
     * [mock 전용] 설계 확정으로 생성된 그룹을 그룹 도메인에 등록한다.
     * <p>
     * 설계 mock의 confirm 시 호출 — 이후 목록/상세/개별상세 조회에 즉시 노출된다(실DB 착각용 상호작용).
     * 그래프(가격 시리즈)는 수급모델 실행 전이라 없음.
     */
    public void registerGroup(ScenarioGroupListItemRes listItem, ScenarioGroupDetailRes detail,
                              List<ScenarioDetailViewRes> scenarioDetails) {
        groups.add(listItem);
        groupDetailMap.put(listItem.scenarioGroupId(), detail);
        scenarioDetails.forEach(d -> scenarioDetailMap.put(d.scenarioId(), d));
        log.info("[Mock] designed group registered: groupId={}, scenarios={}",
            listItem.scenarioGroupId(), scenarioDetails.size());
    }

    private <T> PaginationRes<T> paginate(List<T> items, ScenarioGroupListReq request) {
        int total = items.size();
        int page = request.page();
        int size = request.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<T> pageItems = items.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        return PaginationRes.of(
            totalPages, total, page, pageItems.size(), size,
            page == 1, page >= totalPages,
            page > 1, page < totalPages,
            pageItems
        );
    }

}

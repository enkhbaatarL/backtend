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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    @PostConstruct
    void init() {
        try {
            loadGroups();
            loadGraphs();
            loadGroupDetails();
            loadScenarioDetails();
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
            for (ScenarioRowRes row : group.scenarios()) {
                all.add(ScenarioIndividualItemRes.of(group, row));
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
        List<ScenarioGroupGraphRes> graphs = graphMap.get(scenarioGroupId);
        if (graphs == null || deletedGroupIds.contains(scenarioGroupId)) {
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
                    status, status.getLabel(), s.baseCaseValues(), s.userAdjustedValues());
            })
            .toList();

        return new ScenarioGroupGraphRes(
            graph.scenarioGroupId(), graph.indexName(), graph.periodUnit(), graph.periodUnitLabel(),
            graph.currency(), graph.unit(), graph.dates(), graph.baseSeries(), series);
    }

    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId, GraphPeriodUnit periodUnit) {
        List<ScenarioGroupGraphRes> graphs = graphMap.get(scenarioGroupId);
        if (graphs == null || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: " + scenarioGroupId);
        }
        return ScenarioGroupGraphExcelMapper.toExcel(graphs.stream()
            .filter(g -> periodUnit == g.periodUnit())
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

    public void deleteScenarioGroup(Long scenarioGroupId) {
        boolean exists = groups.stream().anyMatch(g -> g.scenarioGroupId().equals(scenarioGroupId));
        if (!exists || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group not found: " + scenarioGroupId);
        }
        // 그룹 + 그룹 내 모든 시나리오 soft delete (mock: 메모리에 삭제 그룹 기록 → 조회 제외, reset API로 초기화)
        deletedGroupIds.add(scenarioGroupId);
    }

    /** [mock 전용] 상태 변경분과 그룹 삭제분을 모두 초기 설정값으로 되돌린다. */
    public void resetScenarioStatus() {
        statusOverrides.clear();
        deletedGroupIds.clear();
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

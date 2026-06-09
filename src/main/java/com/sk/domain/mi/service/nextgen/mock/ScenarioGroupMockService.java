package com.sk.domain.mi.service.nextgen.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.common.model.TableRes;
import com.sk.common.utils.ExcelUtils;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDetailViewRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupAppliedFilterRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupDetailRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioBulkStatusRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualItemRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioIndividualListRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioRowRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioStatusRes;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<Long, ScenarioStatus> statusOverrides = new ConcurrentHashMap<>();

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

        Comparator<ScenarioGroupListItemRes> comparator = Comparator.comparing(ScenarioGroupListItemRes::createdAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<ScenarioGroupListItemRes> filtered = groups.stream()
            .filter(g -> !deletedGroupIds.contains(g.scenarioGroupId()))
            .filter(g -> startDateTime == null || !g.createdAt().isBefore(startDateTime))
            .filter(g -> endDateTime == null || g.createdAt().isBefore(endDateTime))
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
            Comparator.comparing((ScenarioIndividualItemRes s) -> s.group().createdAt());
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<ScenarioIndividualItemRes> filtered = all.stream()
            .filter(s -> startDateTime == null || !s.group().createdAt().isBefore(startDateTime))
            .filter(s -> endDateTime == null || s.group().createdAt().isBefore(endDateTime))
            .filter(s -> request.keyword().isEmpty() || s.scenarioName().contains(request.keyword()))
            .sorted(comparator)
            .toList();

        return new ScenarioIndividualListRes(ScenarioGroupAppliedFilterRes.from(request), paginate(filtered, request));
    }

    public ScenarioGroupGraphRes getScenarioGroupGraph(Long scenarioGroupId, IndexName indexName) {
        List<ScenarioGroupGraphRes> indexes = graphMap.get(scenarioGroupId);
        if (indexes == null || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: " + scenarioGroupId);
        }
        return indexes.stream()
            .filter(g -> indexName.getCode().equalsIgnoreCase(g.indexName()))
            .findFirst()
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: scenarioGroupId=" + scenarioGroupId + ", indexName=" + indexName));
    }

    public ByteArrayInputStream exportScenarioGroupGraph(Long scenarioGroupId) {
        List<ScenarioGroupGraphRes> indexes = graphMap.get(scenarioGroupId);
        if (indexes == null || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group graph not found: " + scenarioGroupId);
        }
        // 인덱스(JKM/TTF/HH)별로 시트 1개씩
        LinkedHashMap<String, TableRes> sheets = new LinkedHashMap<>();
        for (ScenarioGroupGraphRes index : indexes) {
            sheets.put(index.indexName(), toSheet(index));
        }
        return ExcelUtils.toExcel(sheets);
    }

    /** 한 인덱스 그래프 → 시트(행=시리즈, 열=날짜). 데이터 없으면 빈 칸 */
    private TableRes toSheet(ScenarioGroupGraphRes index) {
        List<String> dates = index.dates();

        // 헤더(열) = 구분 + 날짜들
        List<TableRes.Column> columns = new ArrayList<>();
        columns.add(TableRes.Column.of("series", "구분"));
        for (String date : dates) {
            columns.add(TableRes.Column.of(date, date));
        }

        // 행 = 시리즈별 (Base + 시나리오)
        List<TableRes.Row> rows = new ArrayList<>();
        for (ScenarioGroupGraphRes.BaseSeries base : index.baseSeries()) {
            rows.add(seriesRow(base.seriesName(), base.values(), dates.size()));
        }
        for (ScenarioGroupGraphRes.ScenarioSeries scenario : index.scenarioSeries()) {
            rows.add(seriesRow(scenario.scenarioName(), scenario.values(), dates.size()));
        }
        return new TableRes(index.indexName(), columns, rows);
    }

    private TableRes.Row seriesRow(String name, List<BigDecimal> values, int size) {
        List<String> cells = new ArrayList<>();
        cells.add(name);
        for (int i = 0; i < size; i++) {
            cells.add(valueAt(values, i));
        }
        return TableRes.Row.of(cells);
    }

    /** 값이 없으면 빈 칸 */
    private String valueAt(List<BigDecimal> values, int i) {
        if (values == null || i >= values.size() || values.get(i) == null) {
            return "";
        }
        return values.get(i).toPlainString();
    }

    public ScenarioGroupDetailRes getScenarioGroupDetail(Long scenarioGroupId) {
        ScenarioGroupDetailRes detail = groupDetailMap.get(scenarioGroupId);
        if (detail == null || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group detail not found: " + scenarioGroupId);
        }
        return applyStatusOverrides(detail);
    }

    /** mock 메모리에 저장된 상태 변경분을 그룹 상세에 반영 */
    private ScenarioGroupDetailRes applyStatusOverrides(ScenarioGroupDetailRes detail) {
        if (statusOverrides.isEmpty()) {
            return detail;
        }
        List<ScenarioGroupDetailRowRes> rows = detail.scenarios().stream()
            .map(row -> {
                ScenarioStatus override = statusOverrides.get(row.scenarioId());
                if (override == null) {
                    return row;
                }
                return new ScenarioGroupDetailRowRes(
                    row.scenarioId(), row.scenarioName(), row.baseCase(),
                    row.dimensions(), row.probability(), override, override.getLabel());
            })
            .toList();
        return new ScenarioGroupDetailRes(
            detail.scenarioGroupId(), detail.groupName(), detail.createdAt(),
            detail.baseEvent(), detail.summary(), detail.scenarioCount(),
            detail.dimensionColumns(), rows);
    }

    public ScenarioDetailViewRes getScenarioDetail(Long scenarioGroupId, Long scenarioId) {
        ScenarioDetailViewRes detail = scenarioDetailMap.get(scenarioId);
        if (detail == null
            || (detail.scenarioGroupId() != null && !detail.scenarioGroupId().equals(scenarioGroupId))
            || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario detail not found: scenarioGroupId=" + scenarioGroupId + ", scenarioId=" + scenarioId);
        }
        return detail;
    }

    public ScenarioStatusRes updateScenarioStatus(Long scenarioGroupId, Long scenarioId, ScenarioStatus status) {
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
        return new ScenarioStatusRes(scenarioId, status, status.getLabel());
    }

    public ScenarioBulkStatusRes bulkUpdateScenarioStatus(Long scenarioGroupId, ScenarioStatus status) {
        ScenarioGroupDetailRes detail = groupDetailMap.get(scenarioGroupId);
        if (detail == null || deletedGroupIds.contains(scenarioGroupId)) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario group not found: " + scenarioGroupId);
        }
        // 그룹 전체 시나리오를 동일 상태로 일괄 변경 (mock: 변경분을 메모리에 저장, reset API로 초기화)
        List<Long> scenarioIds = detail.scenarios().stream()
            .map(ScenarioGroupDetailRowRes::scenarioId)
            .toList();
        scenarioIds.forEach(id -> statusOverrides.put(id, status));
        return new ScenarioBulkStatusRes(status, status.getLabel(), scenarioIds.size(), scenarioIds);
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

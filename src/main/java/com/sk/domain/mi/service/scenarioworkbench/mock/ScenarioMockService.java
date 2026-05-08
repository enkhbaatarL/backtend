package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioListReq;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.response.scenarioworkbench.*;
import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.SortOrder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioMockService {

    private final ObjectMapper objectMapper;

    private List<ScenarioListItemRes> scenarioList;
    private Map<Long, ScenarioDetailRes> scenarioDetailMap;

    @PostConstruct
    void init() {
        try {
            loadScenarioList();
            loadScenarioDetails();
            log.info("[Mock] Scenario mock data loaded: list={}, details={}",
                scenarioList.size(), scenarioDetailMap.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load scenario mock data", e);
            scenarioList = Collections.emptyList();
            scenarioDetailMap = Collections.emptyMap();
        }
    }

    private void loadScenarioList() throws IOException {
        try (InputStream is = new ClassPathResource("mock/scenarios/scenario-list.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            scenarioList = objectMapper.treeToValue(
                root.get("scenarios"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ScenarioListItemRes.class)
            );
        }
    }

    private void loadScenarioDetails() throws IOException {
        try (InputStream is = new ClassPathResource("mock/scenarios/scenario-detail.json").getInputStream()) {
            scenarioDetailMap = objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    public ScenarioListRes getScenarios(ScenarioListReq request) {
        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> periodStart = request.period().calcStartDateTime(now, request.startDate());
        Optional<LocalDateTime> periodEnd = request.period().calcEndDateTime(request.endDate());

        Comparator<ScenarioListItemRes> comparator = Comparator.comparing(ScenarioListItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<ScenarioListItemRes> filtered = scenarioList.stream()
            .filter(s -> request.scenarioStatus() == ContentStatus.ALL || s.status() == request.scenarioStatus())
            .filter(s -> periodStart.map(start -> !s.updatedAt().isBefore(start)).orElse(true))
            .filter(s -> periodEnd.map(end -> s.updatedAt().isBefore(end)).orElse(true))
            .filter(s -> request.keyword().isEmpty() || s.scenarioName().contains(request.keyword()))
            .sorted(comparator)
            .toList();

        int total = filtered.size();
        int page = request.page();
        int size = request.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<ScenarioListItemRes> pageItems = filtered.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        PaginationRes<ScenarioListItemRes> pagination = PaginationRes.of(
            totalPages, total, page, pageItems.size(), size,
            page == 1, page >= totalPages,
            page > 1, page < totalPages,
            pageItems
        );

        return new ScenarioListRes(ScenarioAppliedFilterRes.from(request), pagination);
    }

    public ScenarioDetailRes getScenarioDetail(Long scenarioId) {
        ScenarioDetailRes detail = scenarioDetailMap.get(scenarioId);
        if (detail == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Scenario not found: " + scenarioId);
        }
        return detail;
    }

}

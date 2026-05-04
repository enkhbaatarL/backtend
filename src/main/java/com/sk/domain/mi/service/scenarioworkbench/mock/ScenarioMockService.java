package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.*;
import com.sk.domain.mi.service.scenarioworkbench.ScenarioService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("mock")
@RequiredArgsConstructor
public class ScenarioMockService implements ScenarioService {

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

    @Override
    public ScenarioListRes getScenarios(ScenarioListReq request) {
        List<ScenarioListItemRes> filtered = scenarioList.stream()
            .filter(s -> request.keyword().isEmpty() || s.scenarioName().contains(request.keyword()))
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

    @Override
    public ScenarioDetailRes getScenarioDetail(Long scenarioId) {
        ScenarioDetailRes detail = scenarioDetailMap.get(scenarioId);
        if (detail == null) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioId);
        }
        return detail;
    }

}

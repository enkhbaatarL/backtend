package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.OverviewEventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewEventItemRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewPriceForecastRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewScenarioItemRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.OverviewScenarioListRes;
import com.sk.domain.shared.enums.scenarioworkbench.IndexName;
import com.sk.domain.shared.enums.scenarioworkbench.SortOrder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverviewMockService {

    private final ObjectMapper objectMapper;

    private List<OverviewEventItemRes> overviewEventList;
    private List<OverviewScenarioItemRes> overviewScenarioList;
    private List<OverviewPriceForecastRes> overviewPriceForecastList;

    @PostConstruct
    void init() {
        try {
            loadOverviewEvents();
            loadOverviewScenarios();
            loadOverviewPriceForecasts();
            log.info("[Mock] Overview mock data loaded: events={}, scenarios={}, priceForecasts={}",
                overviewEventList.size(), overviewScenarioList.size(), overviewPriceForecastList.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load overview mock data", e);
            overviewEventList = Collections.emptyList();
            overviewScenarioList = Collections.emptyList();
            overviewPriceForecastList = Collections.emptyList();
        }
    }

    private void loadOverviewEvents() throws IOException {
        try (InputStream is = new ClassPathResource("mock/overview/overview-events.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            overviewEventList = objectMapper.treeToValue(
                root.get("events"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, OverviewEventItemRes.class)
            );
        }
    }

    private void loadOverviewScenarios() throws IOException {
        try (InputStream is = new ClassPathResource("mock/overview/overview-scenarios.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            overviewScenarioList = objectMapper.treeToValue(
                root.get("scenarios"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, OverviewScenarioItemRes.class)
            );
        }
    }

    private void loadOverviewPriceForecasts() throws IOException {
        try (InputStream is = new ClassPathResource("mock/overview/overview-price-forecasts.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            overviewPriceForecastList = objectMapper.treeToValue(
                root.get("forecasts"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, OverviewPriceForecastRes.class)
            );
        }
    }

    public OverviewEventListRes getOverviewEvents(OverviewEventListReq request) {
        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        Comparator<OverviewEventItemRes> comparator = Comparator.comparing(OverviewEventItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<OverviewEventItemRes> filtered = overviewEventList.stream()
            .filter(e -> startDateTime == null || !e.updatedAt().isBefore(startDateTime))
            .filter(e -> endDateTime == null || e.updatedAt().isBefore(endDateTime))
            .sorted(comparator)
            .toList();

        int total = filtered.size();
        int page = request.page();
        int size = request.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<OverviewEventItemRes> pageItems = filtered.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        PaginationRes<OverviewEventItemRes> pagination = PaginationRes.of(
            totalPages, total, page, pageItems.size(), size,
            page == 1, page >= totalPages,
            page > 1, page < totalPages,
            pageItems
        );

        return new OverviewEventListRes(pagination);
    }

    public OverviewScenarioListRes getOverviewScenarios(OverviewEventListReq request) {
        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        Comparator<OverviewScenarioItemRes> comparator = Comparator.comparing(OverviewScenarioItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<OverviewScenarioItemRes> filtered = overviewScenarioList.stream()
            .filter(s -> startDateTime == null || !s.updatedAt().isBefore(startDateTime))
            .filter(s -> endDateTime == null || s.updatedAt().isBefore(endDateTime))
            .sorted(comparator)
            .toList();

        int total = filtered.size();
        int page = request.page();
        int size = request.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<OverviewScenarioItemRes> pageItems = filtered.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        PaginationRes<OverviewScenarioItemRes> pagination = PaginationRes.of(
            totalPages, total, page, pageItems.size(), size,
            page == 1, page >= totalPages,
            page > 1, page < totalPages,
            pageItems
        );

        return new OverviewScenarioListRes(pagination);
    }

    public OverviewPriceForecastRes getOverviewPriceForecasts(IndexName indexName, Long eventId, List<Long> scenarioIds) {
        String indexCode = indexName.getCode();
        OverviewPriceForecastRes forecast = overviewPriceForecastList.stream()
            .filter(f -> indexCode.equals(f.indexName()))
            .findFirst()
            .orElse(new OverviewPriceForecastRes(indexCode, "USD", "MMBtu", Collections.emptyList(), Collections.emptyList()));

        if (eventId != null) {
            Set<Long> eventScenarioIds = overviewEventList.stream()
                .filter(e -> e.eventId().equals(eventId))
                .flatMap(e -> e.scenarios().stream())
                .map(s -> s.scenarioId())
                .collect(Collectors.toSet());

            List<OverviewPriceForecastRes.ScenarioForecastLine> filtered = forecast.series().stream()
                .filter(line -> eventScenarioIds.contains(line.scenarioId()))
                .toList();

            return new OverviewPriceForecastRes(forecast.indexName(), forecast.currency(), forecast.unit(), forecast.dates(), filtered);
        }

        if (scenarioIds != null && !scenarioIds.isEmpty()) {
            Set<Long> idSet = Set.copyOf(scenarioIds);

            List<OverviewPriceForecastRes.ScenarioForecastLine> filtered = forecast.series().stream()
                .filter(line -> idSet.contains(line.scenarioId()))
                .toList();

            return new OverviewPriceForecastRes(forecast.indexName(), forecast.currency(), forecast.unit(), forecast.dates(), filtered);
        }

        return forecast;
    }

}

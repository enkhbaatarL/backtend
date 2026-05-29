package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.OverviewEventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.*;
import com.sk.domain.shared.enums.scenarioworkbench.IndexName;
import com.sk.domain.shared.enums.scenarioworkbench.SortOrder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
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
    private final EventMockService eventMockService;
    private final ScenarioMockService scenarioMockService;

    private List<OverviewPriceForecastRes> overviewPriceForecastList;

    // cross-reference maps built from scenario details
    private Map<Long, List<OverviewScenarioBriefRes>> eventScenariosMap;
    private Map<Long, OverviewEventBriefRes> scenarioEventMap;
    private Map<Long, List<String>> scenarioTagsMap;

    @PostConstruct
    void init() {
        try {
            loadOverviewPriceForecasts();
            buildCrossReferences();
            log.info("[Mock] Overview mock data loaded (referencing events/scenarios mock): priceForecasts={}",
                overviewPriceForecastList.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load overview mock data", e);
            overviewPriceForecastList = Collections.emptyList();
            eventScenariosMap = Collections.emptyMap();
            scenarioEventMap = Collections.emptyMap();
            scenarioTagsMap = Collections.emptyMap();
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

    private void buildCrossReferences() {
        Map<Long, ScenarioDetailRes> detailMap = scenarioMockService.getAllScenarioDetails();

        Map<Long, Map<Long, OverviewScenarioBriefRes>> eventScenarios = new HashMap<>();
        Map<Long, OverviewEventBriefRes> scenarioEvent = new HashMap<>();
        Map<Long, List<String>> scenarioTags = new HashMap<>();

        for (ScenarioDetailRes detail : detailMap.values()) {
            scenarioTags.put(detail.scenarioId(),
                detail.tags() != null ? detail.tags() : Collections.emptyList());

            RelatedEventScenarioRes related = detail.relatedEventScenario();
            if (related == null || related.event() == null) {
                continue;
            }

            RelatedEventRes event = related.event();

            scenarioEvent.put(detail.scenarioId(), new OverviewEventBriefRes(
                event.eventId(), event.eventName(),
                event.createdAt(), event.updatedAt(),
                event.eventTypeLv1(), event.eventTypeLv1Label(),
                event.indexImpacts()
            ));

            if (related.scenarios() != null) {
                Map<Long, OverviewScenarioBriefRes> scenariosById =
                    eventScenarios.computeIfAbsent(event.eventId(), ignored -> new LinkedHashMap<>());

                related.scenarios().forEach(s -> scenariosById.putIfAbsent(
                    s.scenarioId(),
                    new OverviewScenarioBriefRes(
                        s.scenarioId(), s.scenarioName(), s.baseCase(),
                        s.indexImpacts(), s.probability()
                    )
                ));
            }
        }

        this.eventScenariosMap = eventScenarios.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().values().stream()
                    .sorted(Comparator.comparing(OverviewScenarioBriefRes::scenarioId))
                    .toList()
            ));
        this.scenarioEventMap = scenarioEvent;
        this.scenarioTagsMap = scenarioTags;
    }

    public OverviewEventListRes getOverviewEvents(OverviewEventListReq request) {
        List<EventListItemRes> allEvents = eventMockService.getEventListItems();

        List<OverviewEventItemRes> overviewEvents = allEvents.stream()
            .map(e -> new OverviewEventItemRes(
                e.eventId(), e.eventName(), e.createdAt(), e.updatedAt(),
                e.eventTypeLv1(), e.eventTypeLv1Label(),
                e.signalType(), e.signalTypeLabel(), e.indexImpacts(),
                eventScenariosMap.getOrDefault(e.eventId(), Collections.emptyList())
            ))
            .toList();

        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        Comparator<OverviewEventItemRes> comparator = Comparator.comparing(OverviewEventItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<OverviewEventItemRes> filtered = overviewEvents.stream()
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
        List<ScenarioListItemRes> allScenarios = scenarioMockService.getScenarioListItems();

        List<OverviewScenarioItemRes> overviewScenarios = allScenarios.stream()
            .map(s -> {
                OverviewEventBriefRes eventBrief = scenarioEventMap.get(s.scenarioId());
                List<OverviewEventBriefRes> events = eventBrief != null
                    ? List.of(eventBrief)
                    : Collections.emptyList();
                List<String> tags = scenarioTagsMap.getOrDefault(s.scenarioId(), Collections.emptyList());
                return new OverviewScenarioItemRes(
                    s.scenarioId(), s.scenarioName(), s.createdAt(), s.updatedAt(),
                    tags, s.baseCase(), s.indexImpacts(), s.probability(), events
                );
            })
            .toList();

        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        Comparator<OverviewScenarioItemRes> comparator = Comparator.comparing(OverviewScenarioItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<OverviewScenarioItemRes> filtered = overviewScenarios.stream()
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
            Set<Long> eventScenarioIds = eventScenariosMap.getOrDefault(eventId, Collections.emptyList()).stream()
                .map(OverviewScenarioBriefRes::scenarioId)
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

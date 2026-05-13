package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.DesignEventListReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenCallbackReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ScenarioGenReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.DesignAppliedFilterRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.DesignEventItemRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.DesignEventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.GeneratedScenarioRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenAcceptRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ScenarioGenStatusRes;
import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.ScenarioGenStatus;
import com.sk.domain.shared.enums.scenarioworkbench.SortOrder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DesignMockService {

    private static final long AUTO_COMPLETE_DELAY_SECONDS = 5;
    private static final String SCENARIO_TEMPLATE_KEY = "template";

    private final ObjectMapper objectMapper;
    private final AtomicLong requestIdSeq = new AtomicLong(200);
    private final ConcurrentHashMap<Long, MockGenRequest> requestStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private List<DesignEventItemRes> eventList;
    private Map<String, List<GeneratedScenarioRes>> generatedScenariosMap;

    @PostConstruct
    void init() {
        loadAll();
    }

    @PreDestroy
    void destroy() {
        scheduler.shutdownNow();
    }

    private void loadAll() {
        try {
            loadEventList();
            loadGeneratedScenarios();
            requestStore.clear();
            requestIdSeq.set(200);
            initRequestStore();
            log.info("[Mock] Design mock data loaded: list={}, generatedScenarios={}, requestStore={}",
                eventList.size(), generatedScenariosMap.size(), requestStore.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load design mock data", e);
            eventList = Collections.emptyList();
            generatedScenariosMap = Collections.emptyMap();
        }
    }

    private void initRequestStore() {
        LocalDateTime now = LocalDateTime.now();
        for (DesignEventItemRes event : eventList) {
            if (event.requestId() != null) {
                requestStore.put(event.requestId(),
                    new MockGenRequest(event.requestId(), event.eventId(), event.scenarioGenStatus(), now));
            }
        }
    }

    private void loadEventList() throws IOException {
        try (InputStream is = new ClassPathResource("mock/design/design-event-list.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            eventList = new ArrayList<>(objectMapper.treeToValue(
                root.get("events"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, DesignEventItemRes.class)
            ));
        }
    }

    private void loadGeneratedScenarios() throws IOException {
        try (InputStream is = new ClassPathResource("mock/design/design-generated-scenarios.json").getInputStream()) {
            generatedScenariosMap = new ConcurrentHashMap<>(
                objectMapper.readValue(is, new TypeReference<Map<String, List<GeneratedScenarioRes>>>() {}));
        }
    }

    public void reset() {
        loadAll();
        log.info("[Mock] Design mock data reset to initial state");
    }

    public DesignEventListRes getDesignEvents(DesignEventListReq request) {
        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        Comparator<DesignEventItemRes> comparator = Comparator.comparing(DesignEventItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<DesignEventItemRes> filtered = eventList.stream()
            .filter(e -> request.eventStatus() == ContentStatus.ALL || e.status() == request.eventStatus())
            .filter(e -> request.eventCategoryLv1().isEmpty() || request.eventCategoryLv1().contains(e.eventCategoryLv1()))
            .filter(e -> startDateTime == null || !e.updatedAt().isBefore(startDateTime))
            .filter(e -> endDateTime == null || e.updatedAt().isBefore(endDateTime))
            .filter(e -> request.keyword().isEmpty() || e.eventName().contains(request.keyword()))
            .filter(e -> !request.hideCompleted() || e.scenarioGenStatus() != ScenarioGenStatus.COMPLETED)
            .filter(e -> !request.hideNonRecommended() || Boolean.TRUE.equals(e.scenarioRecommended()))
            .sorted(comparator)
            .toList();

        int total = filtered.size();
        int page = request.page();
        int size = request.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<DesignEventItemRes> pageItems = filtered.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        PaginationRes<DesignEventItemRes> pagination = PaginationRes.of(
            totalPages, total, page, pageItems.size(), size,
            page == 1, page >= totalPages,
            page > 1, page < totalPages,
            pageItems
        );

        return new DesignEventListRes(DesignAppliedFilterRes.from(request), pagination);
    }

    public ScenarioGenAcceptRes generateScenarios(ScenarioGenReq request) {
        Long requestId = requestIdSeq.getAndIncrement();
        LocalDateTime now = LocalDateTime.now();

        for (Long eventId : request.eventIds()) {
            requestStore.put(requestId, new MockGenRequest(requestId, eventId, ScenarioGenStatus.PROCESSING, now));
            updateEventStatus(eventId, ScenarioGenStatus.PROCESSING, requestId, now);
        }

        scheduleAutoComplete(requestId);

        return new ScenarioGenAcceptRes(
            requestId,
            ScenarioGenStatus.PROCESSING,
            ScenarioGenStatus.PROCESSING.getLabel(),
            now  // scenarioGenStartedAt
        );
    }

    private void scheduleAutoComplete(Long requestId) {
        scheduler.schedule(() -> {
            MockGenRequest req = requestStore.get(requestId);
            if (req != null && req.status() == ScenarioGenStatus.PROCESSING) {
                requestStore.put(requestId,
                    new MockGenRequest(req.requestId(), req.eventId(), ScenarioGenStatus.COMPLETED, req.requestedAt()));
                updateEventStatus(req.eventId(), ScenarioGenStatus.COMPLETED, requestId, req.requestedAt());

                List<GeneratedScenarioRes> template = generatedScenariosMap.getOrDefault(
                    SCENARIO_TEMPLATE_KEY, Collections.emptyList());
                if (!template.isEmpty()) {
                    generatedScenariosMap.put(String.valueOf(requestId), template);
                }

                log.info("[Mock] Auto-completed scenario generation: requestId={}, eventId={}", requestId, req.eventId());
            }
        }, AUTO_COMPLETE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void updateEventStatus(Long eventId, ScenarioGenStatus status, Long requestId, LocalDateTime startedAt) {
        eventList = new ArrayList<>(eventList.stream()
            .map(e -> e.eventId().equals(eventId)
                ? new DesignEventItemRes(
                    e.eventId(), e.eventName(), e.status(), e.statusLabel(),
                    e.eventCategoryLv1(), e.eventCategoryLv1Label(),
                    e.createdAt(), e.updatedAt(), e.scenarioRecommended(), e.recommendationReason(), e.indexImpacts(),
                    status, status.getLabel(), startedAt, requestId)
                : e)
            .toList());
    }

    public void handleCallback(ScenarioGenCallbackReq request) {
        MockGenRequest req = requestStore.get(request.requestId());
        if (req != null && req.status() == ScenarioGenStatus.PROCESSING) {
            requestStore.put(req.requestId(),
                new MockGenRequest(req.requestId(), req.eventId(), ScenarioGenStatus.COMPLETED, req.requestedAt()));
            updateEventStatus(req.eventId(), ScenarioGenStatus.COMPLETED, req.requestId(), req.requestedAt());

            List<GeneratedScenarioRes> template = generatedScenariosMap.getOrDefault(
                SCENARIO_TEMPLATE_KEY, Collections.emptyList());
            if (!template.isEmpty()) {
                generatedScenariosMap.put(String.valueOf(req.requestId()), template);
            }
        }
        log.info("[Mock] Callback processed: requestId={}, success={}", request.requestId(), request.success());
    }

    public List<GeneratedScenarioRes> getGeneratedScenarios(Long requestId) {
        return generatedScenariosMap.getOrDefault(String.valueOf(requestId), Collections.emptyList());
    }

    public List<ScenarioGenStatusRes> getGenerateStatuses(List<Long> requestIds) {
        return requestIds.stream()
            .map(requestStore::get)
            .filter(r -> r != null)
            .map(r -> new ScenarioGenStatusRes(r.requestId(), r.eventId(), r.status(), r.status().getLabel(), r.requestedAt()))
            .toList();
    }

    private record MockGenRequest(Long requestId, Long eventId, ScenarioGenStatus status, LocalDateTime requestedAt) {}

}

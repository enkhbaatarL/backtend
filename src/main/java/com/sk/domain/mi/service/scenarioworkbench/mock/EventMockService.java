package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventAppliedFilterRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventDetailRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventListItemRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventListRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.EventRelatedRecordRes;
import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.EventSignalType;
import com.sk.domain.shared.enums.scenarioworkbench.SortOrder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventMockService {

    private static final Map<String, Long> EVENT_TYPE_CODE_TO_ID = Map.of(
        "geopolitical_risk", 1L,
        "weather_climate", 2L,
        "infrastructure_facility", 3L,
        "supply_demand_balance", 4L,
        "logistics_shipping", 5L,
        "energy_mix_transition", 6L,
        "macroeconomy_policy", 7L,
        "market_trading_structure", 8L
    );

    private final ObjectMapper objectMapper;

    private List<EventListItemRes> eventList;
    private Map<Long, EventDetailRes> eventDetailMap;
    private Map<Long, List<EventRelatedRecordRes>> eventRecordsMap;
    private Map<Long, List<EventInstitutionOutlookRes>> eventOutlooksMap;

    @PostConstruct
    void init() {
        try {
            loadEventList();
            loadEventDetails();
            loadEventRecords();
            loadEventOutlooks();
            log.info("[Mock] Event mock data loaded: list={}, details={}, records={}, outlooks={}",
                eventList.size(), eventDetailMap.size(), eventRecordsMap.size(), eventOutlooksMap.size());
        } catch (IOException e) {
            log.error("[Mock] Failed to load event mock data", e);
            eventList = Collections.emptyList();
            eventDetailMap = Collections.emptyMap();
            eventRecordsMap = Collections.emptyMap();
            eventOutlooksMap = Collections.emptyMap();
        }
    }

    private void loadEventList() throws IOException {
        try (InputStream is = new ClassPathResource("mock/events/event-list.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            eventList = objectMapper.treeToValue(
                root.get("events"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, EventListItemRes.class)
            );
        }
    }

    private void loadEventDetails() throws IOException {
        try (InputStream is = new ClassPathResource("mock/events/event-detail.json").getInputStream()) {
            eventDetailMap = objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    private void loadEventRecords() throws IOException {
        try (InputStream is = new ClassPathResource("mock/events/event-records.json").getInputStream()) {
            eventRecordsMap = objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    private void loadEventOutlooks() throws IOException {
        try (InputStream is = new ClassPathResource("mock/events/event-outlooks.json").getInputStream()) {
            eventOutlooksMap = objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    public EventListRes getEvents(EventListReq request) {
        LocalDateTime startDateTime = request.startDate() != null ? request.startDate().atStartOfDay() : null;
        LocalDateTime endDateTime = request.endDate() != null ? request.endDate().plusDays(1).atStartOfDay() : null;

        Comparator<EventListItemRes> comparator = Comparator.comparing(EventListItemRes::updatedAt);
        if (request.sort() == SortOrder.LATEST) {
            comparator = comparator.reversed();
        }

        List<EventListItemRes> filtered = eventList.stream()
            .filter(e -> request.eventStatus() == ContentStatus.ALL || e.status() == request.eventStatus())
            .filter(e -> request.eventStatus() == ContentStatus.ALL || e.signalType() == EventSignalType.SIGNAL)
            .filter(e -> !request.suggestedOnly() || Boolean.TRUE.equals(e.isScenarioSuggested()))
            .filter(e -> matchesEventTypeFilter(e.eventTypeLv1(), request.eventTypeL1Id()))
            .filter(e -> startDateTime == null || !e.updatedAt().isBefore(startDateTime))
            .filter(e -> endDateTime == null || e.updatedAt().isBefore(endDateTime))
            .filter(e -> request.keyword().isEmpty() || e.eventName().contains(request.keyword()))
            .sorted(comparator)
            .toList();

        int total = filtered.size();
        int page = request.page();
        int size = request.size();
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<EventListItemRes> pageItems = filtered.subList(fromIndex, toIndex);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        PaginationRes<EventListItemRes> pagination = PaginationRes.of(
            totalPages, total, page, pageItems.size(), size,
            page == 1, page >= totalPages,
            page > 1, page < totalPages,
            pageItems
        );

        return new EventListRes(EventAppliedFilterRes.from(request), pagination);
    }

    public List<EventListItemRes> getEventListItems() {
        return eventList;
    }

    public EventDetailRes getEventDetail(Long eventId) {
        EventDetailRes detail = eventDetailMap.get(eventId);
        if (detail == null) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "Event not found: " + eventId);
        }
        return detail;
    }

    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        List<EventRelatedRecordRes> records = eventRecordsMap.getOrDefault(eventId, Collections.emptyList());
        return paginate(records, page, size);
    }

    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        List<EventInstitutionOutlookRes> outlooks = eventOutlooksMap.getOrDefault(eventId, Collections.emptyList());
        return paginate(outlooks, page, size);
    }

    private boolean matchesEventTypeFilter(String eventTypeCode, List<Long> filterIds) {
        if (filterIds == null || filterIds.isEmpty()) {
            return true;
        }
        Long id = EVENT_TYPE_CODE_TO_ID.get(eventTypeCode);
        return id != null && filterIds.contains(id);
    }

    private <T> PaginationRes<T> paginate(List<T> items, int page, int size) {
        if (items == null) {
            items = Collections.emptyList();
        }
        int total = items.size();
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

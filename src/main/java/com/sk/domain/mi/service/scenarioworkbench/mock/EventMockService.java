package com.sk.domain.mi.service.scenarioworkbench.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.*;
import com.sk.domain.mi.service.scenarioworkbench.EventService;
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
public class EventMockService implements EventService {

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

    @Override
    public EventListRes getEvents(EventListReq request) {
        List<EventListItemRes> filtered = eventList.stream()
            .filter(e -> request.eventCategoryLv1().isEmpty() || request.eventCategoryLv1().contains(e.eventCategoryLv1()))
            .filter(e -> request.keyword().isEmpty() || e.title().contains(request.keyword()))
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

    @Override
    public EventDetailRes getEventDetail(Long eventId) {
        EventDetailRes detail = eventDetailMap.get(eventId);
        if (detail == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        return detail;
    }

    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        List<EventRelatedRecordRes> records = eventRecordsMap.getOrDefault(eventId, Collections.emptyList());
        return paginate(records, page, size);
    }

    @Override
    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        List<EventInstitutionOutlookRes> outlooks = eventOutlooksMap.getOrDefault(eventId, Collections.emptyList());
        return paginate(outlooks, page, size);
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

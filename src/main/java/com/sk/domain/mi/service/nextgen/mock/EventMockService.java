package com.sk.domain.mi.service.nextgen.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.mi.dto.response.nextgen.EventAppliedFilterRes;
import com.sk.domain.mi.dto.response.nextgen.EventDetailRes;
import com.sk.domain.mi.dto.response.nextgen.EventInstitutionOutlookRes;
import com.sk.domain.mi.dto.response.nextgen.EventListItemRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.design.EventDesignAvailabilityRes;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.enums.nextgen.SortOrder;
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
        "supply", 1L,
        "demand", 2L,
        "inventory_logistics", 3L,
        "exogenous_shock", 4L
    );

    private final ObjectMapper objectMapper;
    private final ScenarioDesignMockService designMockService;
    private final ScenarioGroupMockService scenarioGroupMockService;

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

        Comparator<EventListItemRes> comparator = Comparator.comparing(EventListItemRes::latestRecordFirstCreated);
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
        List<EventListItemRes> pageItems = filtered.subList(fromIndex, toIndex).stream()
            .map(this::overlayLiveDesign)
            .toList();
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
        // 상세에는 scenarioGroupId 필드가 없어 그룹삭제 판정은 생략(목록에서 처리), 라이브 세션 상태만 반영
        DesignDisplay design = resolveDesignDisplay(eventId, detail.scenarioDesignStatus(), null);
        ScenarioDesignDisplayStatus status = design.status();
        boolean occupied = status == ScenarioDesignDisplayStatus.CREATING;
        // mock은 점유자명 소스가 없어 occupantUserName은 null (실DB 경로에서만 채움)
        return new EventDetailRes(
            detail.eventId(), detail.eventName(), detail.status(), detail.statusLabel(),
            detail.createdAt(), detail.updatedAt(), detail.latestRecordFirstCreated(),
            detail.signalType(), detail.signalTypeLabel(), detail.isScenarioSuggested(), detail.scenarioSuggestReason(),
            status, status == null ? null : status.getLabel(),
            occupied, occupied ? design.occupantUserId() : null, null,
            detail.aiInsights(), detail.impactAnalysis(), detail.driverTreeScoreboard(), detail.institutionViewSynthesis(), detail.relatedEventScenario());
    }

    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        List<EventRelatedRecordRes> records = eventRecordsMap.getOrDefault(eventId, Collections.emptyList());
        return paginate(records, page, size);
    }

    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        List<EventInstitutionOutlookRes> outlooks = eventOutlooksMap.getOrDefault(eventId, Collections.emptyList());
        return paginate(outlooks, page, size);
    }

    /**
     * 이벤트 설계가능 상태 조회 — 라이브 설계 mock 세션 우선, 없으면 이벤트의 정적 상태(사전 그룹) 반영.
     * <p>
     * 라이브 세션: creating=설계중(점유) / created=설계완료. 세션 없음: event-list의 scenarioDesignStatus·scenarioGroupId
     * 를 그대로 따른다(= created 이벤트는 created로 일관). eventIds 미지정 시 전체 이벤트 대상.
     */
    public List<EventDesignAvailabilityRes> getEventDesignAvailability(List<Long> eventIds) {
        List<Long> targets = (eventIds == null || eventIds.isEmpty())
            ? eventList.stream().map(EventListItemRes::eventId).toList()
            : eventIds;
        return targets.stream().map(this::buildDesignAvailability).toList();
    }

    private EventDesignAvailabilityRes buildDesignAvailability(Long eventId) {
        // ① 라이브 설계 세션이 있으면 그 상태(설계중/완료)를 우선 반영
        var session = designMockService.findSessionByEventId(eventId);
        if (session.isPresent()) {
            ScenarioDesignMockService.DesignSessionInfo info = session.get();
            ScenarioDesignDisplayStatus status = ScenarioDesignDisplayStatus.of(info.status(), true);
            boolean occupied = info.status() == ScenarioGroupStatus.CREATING;
            return EventDesignAvailabilityRes.of(
                eventId, status, occupied, occupied ? info.occupantUserId() : null, null, info.scenarioGroupId());
        }
        // ② 세션 없으면 이벤트의 정적 설계 상태(사전 그룹) — event-list와 일관
        EventListItemRes event = eventById(eventId);
        if (event == null || event.scenarioDesignStatus() == null) {
            return EventDesignAvailabilityRes.designable(eventId);
        }
        // 그룹이 삭제됐으면 그 이벤트는 다시 설계가능 (그룹 삭제 ↔ 이벤트 가용성 유기적 연동)
        if (scenarioGroupMockService.isGroupDeleted(event.scenarioGroupId())) {
            return EventDesignAvailabilityRes.designable(eventId);
        }
        return EventDesignAvailabilityRes.of(
            eventId, event.scenarioDesignStatus(), false, null, null, event.scenarioGroupId());
    }

    /**
     * 이벤트의 라이브 설계 상태(목록/상세 표시용)를 계산한다.
     * <p>① 라이브 설계 세션이 있으면 그 상태(설계중/완료)·그룹ID, ② 그룹이 삭제됐으면 설계가능,
     * ③ 그 외엔 정적 fixture 값을 그대로 둔다. (이벤트↔설계↔그룹 유기적 연동)
     */
    private DesignDisplay resolveDesignDisplay(Long eventId, ScenarioDesignDisplayStatus staticStatus, Long staticGroupId) {
        var session = designMockService.findSessionByEventId(eventId);
        if (session.isPresent()) {
            ScenarioDesignMockService.DesignSessionInfo info = session.get();
            ScenarioDesignDisplayStatus status = ScenarioDesignDisplayStatus.of(info.status(), true);
            // 점유 = 설계중(creating). 점유자는 세션의 실제 시작 사용자.
            Long occupant = status == ScenarioDesignDisplayStatus.CREATING ? info.occupantUserId() : null;
            return new DesignDisplay(status, info.scenarioGroupId(), occupant);
        }
        if (scenarioGroupMockService.isGroupDeleted(staticGroupId)) {
            return new DesignDisplay(ScenarioDesignDisplayStatus.DESIGNABLE, null, null);
        }
        // 정적 fixture는 점유자 정보가 없음 — 라이브 세션이 있을 때만 점유자를 안다.
        return new DesignDisplay(staticStatus, staticGroupId, null);
    }

    private record DesignDisplay(ScenarioDesignDisplayStatus status, Long scenarioGroupId, Long occupantUserId) {}

    /** 목록 아이템에 라이브 설계 상태(상태/그룹/점유)를 덮어쓴 새 아이템을 만든다 (DTO는 그대로, 조립은 mock에서). */
    private EventListItemRes overlayLiveDesign(EventListItemRes item) {
        DesignDisplay design = resolveDesignDisplay(item.eventId(), item.scenarioDesignStatus(), item.scenarioGroupId());
        ScenarioDesignDisplayStatus status = design.status();
        boolean occupied = status == ScenarioDesignDisplayStatus.CREATING;
        return new EventListItemRes(
            item.eventId(), item.eventName(), item.status(), item.statusLabel(),
            item.eventTypeLv1(), item.eventTypeLv1Label(), item.createdAt(), item.updatedAt(),
            item.latestRecordFirstCreated(), item.signalType(), item.signalTypeLabel(),
            item.isScenarioSuggested(), item.scenarioSuggestReason(),
            status, status == null ? null : status.getLabel(),
            design.scenarioGroupId(), occupied, design.occupantUserId(), null, item.indexImpacts());
    }

    private EventListItemRes eventById(Long eventId) {
        return eventList.stream()
            .filter(e -> eventId.equals(e.eventId()))
            .findFirst()
            .orElse(null);
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

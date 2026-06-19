package com.sk.domain.mi.service.nextgen;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.sk.domain.mi.dto.response.nextgen.EventRelatedScenarioRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedScenariosRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.design.EventDesignAvailabilityRes;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventTypeL1Repository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.EventDesignAvailabilityRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final MiEventRepository miEventRepository;
    private final MiEventTypeL1Repository miEventTypeL1Repository;
    private final MiScenarioGroupRepository miScenarioGroupRepository;
    private final UserRepository userRepository;

    /**
     * [이벤트 유형 L1 목록]
     */
    @Override
    public List<EventTypeL1Res> getEventTypesL1() {
        return miEventTypeL1Repository.findAllByIsDeletedFalseOrderByEventTypeL1IdAsc()
            .stream()
            .map(EventTypeL1Res::from)
            .toList();
    }

    /**
     * [이벤트 목록]
     * - 최신순 정렬, 기간필터 latest_record_first_created 기준 (수정일시 아님 — 최신 연관 레코드의 최초 생성일시)
     * - suggestedOnly=true면 시나리오화 추천 이벤트만 조회
     * - scenarioDesignStatus는 이벤트:활성 그룹 = 1:1 left join에서 파생 — 그룹 없으면 designable(설계 버튼),
     *   그룹 status creating이면 creating(생성 중), created/realized면 created(+ scenarioGroupId)
     */
    @Override
    public EventListRes getEvents(EventListReq request) {
        Page<EventListRow> page = miEventRepository.getEventsByConditions(request);

        // 점유(설계중) 이벤트의 요청자 userId만 모아 사용자명 일괄 조회
        Map<Long, String> occupantNames = resolveOccupantNames(
            page.getContent().stream()
                .filter(row -> row.scenarioGroupStatus() == ScenarioGroupStatus.CREATING)
                .map(EventListRow::requestUserId)
                .toList());

        return new EventListRes(
            EventAppliedFilterRes.from(request),
            PaginationRes.toRes(page.map(row -> EventListItemRes.from(row, occupantNames.get(row.requestUserId()))))
        );
    }

    /**
     * [이벤트 상세]
     * - relatedEventScenario(연관 시나리오)는 활성 시나리오 그룹이 없으면 null
     * - scenarioDesignStatus 파생 규칙은 목록과 동일 (활성 그룹 status 기반)
     */
    @Override
    public EventDetailRes getEventDetail(Long eventId) {
        EventDetailRow row = miEventRepository.findEventDetail(eventId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Event not found: %d".formatted(eventId)));

        return EventDetailRes.from(row, buildRelatedScenarios(row), resolveOccupantName(row.relatedScenarioGroup()));
    }

    /** 활성 그룹이 설계중(점유)일 때만 그룹 요청자의 사용자명을 조회한다. (그 외 null) */
    private String resolveOccupantName(ScenarioGroupBaseRow group) {
        if (group == null || group.status() != ScenarioGroupStatus.CREATING || group.requestUserId() == null) {
            return null;
        }
        return resolveOccupantNames(List.of(group.requestUserId())).get(group.requestUserId());
    }

    /**
     * 점유자(설계중 그룹 요청자) userId → 사용자명 매핑.
     * requestUserId(Long)를 사용자 PK(usr_no, Integer)로 조회하며, 삭제/미존재 사용자는 결과에서 빠진다(이름 null).
     */
    private Map<Long, String> resolveOccupantNames(Collection<Long> userIds) {
        List<Integer> ids = userIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(Long::intValue)
            .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByUserNoInAndIsDeletedFalse(ids).stream()
            .collect(Collectors.toMap(user -> user.getUserNo().longValue(), User::getUserNm));
    }

    /**
     * 상세의 연관 시나리오 표 조립
     * - 표 헤더/차원값은 시나리오 그룹 도메인과 동일하게 event_type_l2 template + dimension_selected 키로 구성
     * - adjustedProbability도 그룹 도메인과 동일한 재분배 규칙 (조회 시점 계산, 비영속)
     */
    private EventRelatedScenariosRes buildRelatedScenarios(EventDetailRow row) {
        ScenarioGroupBaseRow group = row.relatedScenarioGroup();

        if (group == null) {
            return null;
        }

        GroupDimensionTemplate template = ScenarioDimensionMapper.parseTemplates(miScenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(group.eventTypeL2Id())))
            .getOrDefault(group.eventTypeL2Id(), GroupDimensionTemplate.EMPTY);

        List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(group.dimensionSelected());

        List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
        Map<Long, Integer> adjusted = ScenarioProbabilityAllocator.redistributeRows(row.relatedScenarios());
        List<EventRelatedScenarioRes> scenarios = row.relatedScenarios().stream()
            .map(s -> EventRelatedScenarioRes.of(
                s,
                template.dimensions(selectedKeys, s.dimensionValues()),
                adjusted.get(s.scenarioId())
            ))
            .toList();

        return EventRelatedScenariosRes.of(group, dimensionColumns, scenarios);
    }

    /**
     * [기관 전망 목록]
     * - 전망 일자(view_date) 내림차순, 동일 일자는 ID 내림차순
     */
    @Override
    public PaginationRes<EventInstitutionOutlookRes> getEventOutlooks(Long eventId, int page, int size) {
        return PaginationRes.toRes(
            miEventRepository.findEventOutlooks(eventId, page, size).map(EventInstitutionOutlookRes::from)
        );
    }

    /**
     * [연관 레코드 목록]
     * - 뉴스(NEWS)와 데이터 파일 요약(비NEWS) 두 소스를 UNION으로 합쳐 first_created 내림차순
     */
    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        return PaginationRes.toRes(
            miEventRepository.findEventRecords(eventId, PageRequest.of(page - 1, size)).map(EventRelatedRecordRes::from)
        );
    }

    /**
     * [이벤트 설계 점유/가능 상태] (이벤트 목록 폴링)
     *  - 요청한 이벤트별로 활성 그룹 status에서 설계 상태를 파생한다 — 그룹 없음=designable, creating=설계중(점유), created/realized=설계완료.
     *  - 점유 판정은 그룹 row 자체를 lock으로 처리
     */
    @Override
    public List<EventDesignAvailabilityRes> getEventDesignAvailability(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) return List.of();

        Map<Long, EventDesignAvailabilityRow> rowByEventId = miEventRepository.findDesignAvailability(eventIds).stream()
            .collect(Collectors.toMap(EventDesignAvailabilityRow::miEventId, Function.identity()));

        // 점유(설계중) 이벤트의 요청자 userId만 모아 사용자명 일괄 조회
        Map<Long, String> occupantNames = resolveOccupantNames(
            rowByEventId.values().stream()
                .filter(row -> row.scenarioGroupStatus() == ScenarioGroupStatus.CREATING)
                .map(EventDesignAvailabilityRow::requestUserId)
                .toList());

        return eventIds.stream()
            .distinct()
            .map(eventId -> toAvailability(eventId, rowByEventId.get(eventId), occupantNames))
            .toList();
    }

    private EventDesignAvailabilityRes toAvailability(Long eventId, EventDesignAvailabilityRow row, Map<Long, String> occupantNames) {
        if (row == null) return EventDesignAvailabilityRes.designable(eventId);
        ScenarioDesignDisplayStatus status = ScenarioDesignDisplayStatus.of(row.scenarioGroupStatus(), row.templateAvailable());
        boolean occupied = row.scenarioGroupStatus() == ScenarioGroupStatus.CREATING;
        return EventDesignAvailabilityRes.of(
            eventId, status, occupied,
            occupied ? row.requestUserId() : null,
            occupied ? occupantNames.get(row.requestUserId()) : null,
            row.scenarioGroupId());
    }

}

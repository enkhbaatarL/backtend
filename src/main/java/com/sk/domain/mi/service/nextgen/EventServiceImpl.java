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
import com.sk.domain.mi.dto.response.nextgen.EventLinkedScenarioGroupRes;
import com.sk.domain.mi.dto.response.nextgen.EventListItemRes;
import com.sk.domain.mi.dto.response.nextgen.EventListRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedRecordRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedScenarioRes;
import com.sk.domain.mi.dto.response.nextgen.EventRelatedScenariosRes;
import com.sk.domain.mi.dto.response.nextgen.EventTypeL1Res;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.design.EventDesignAvailabilityRes;
import com.sk.domain.mi.service.nextgen.support.FailedCreatingGroupResolver;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiEventTypeL1Repository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.EventDesignAvailabilityRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventLinkedScenarioGroupRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final FailedCreatingGroupResolver failedCreatingGroupResolver;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

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
     * - 멀티 이벤트 전환으로 이벤트:활성 그룹은 N건 — 시나리오 표기는 scenarioGroupCount만 제공하고
     *   (0이면 설계 아이콘, 1 이상이면 그룹 N 버튼), N건 상세는 연결 그룹 패널 API가 담당
     */
    @Override
    public EventListRes getEvents(EventListReq request) {
        Page<EventListRow> page = miEventRepository.getEventsByConditions(request);

        return new EventListRes(
            EventAppliedFilterRes.from(request),
            PaginationRes.toRes(page.map(EventListItemRes::from))
        );
    }

    /**
     * [이벤트 상세]
     * - relatedEventScenarios(연관 시나리오)는 연결된 그룹별 표를 그룹 오래된 순으로 스택 (없으면 빈 목록)
     * - 설계 상태/점유 헤더는 제공하지 않는다 — 조합 단위 판정인 이벤트 조합 설계 상태 조회 API가 담당 (목록과 동일 정책)
     */
    @Override
    public EventDetailRes getEventDetail(Long eventId) {
        EventDetailRow row = miEventRepository.findEventDetail(eventId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Event not found: %d".formatted(eventId)));

        return EventDetailRes.from(row, buildRelatedScenarios(row));
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
            return Collections.emptyMap();
        }

        return userRepository.findByUserNoInAndIsDeletedFalse(ids).stream()
            .collect(Collectors.toMap(user -> user.getUserNo().longValue(), User::getUserNm));
    }

    /**
     * 상세의 연관 시나리오 표 조립 (그룹별 표 스택, 그룹 오래된 순)
     * - 표 헤더/차원값은 시나리오 그룹 도메인과 동일 (v4는 dimension_definitions, v1은 event_type_l2 template)
     * - adjustedProbability도 그룹 도메인과 동일한 재분배 규칙 (그룹 단위 계산, 비영속)
     * - [260621] 설계(생성) 진행 중인 그룹은 미완성이라 미노출 — 실패 방치 그룹도 creating이라 함께 걸러진다
     */
    private List<EventRelatedScenariosRes> buildRelatedScenarios(EventDetailRow row) {
        List<ScenarioGroupBaseRow> groups = row.relatedScenarioGroups().stream()
            .filter(g -> g.status() != ScenarioGroupStatus.CREATING)
            .toList();
        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> eventTypeL2Ids = groups.stream()
            .map(ScenarioGroupBaseRow::eventTypeL2Id)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, GroupDimensionTemplate> templates = ScenarioDimensionMapper.parseTemplates(
            miScenarioGroupRepository.findTemplatesByEventTypeL2Ids(eventTypeL2Ids));

        Map<Long, List<ScenarioBaseRow>> scenariosByGroup = row.relatedScenarios().stream()
            .collect(Collectors.groupingBy(ScenarioBaseRow::scenarioGroupId, LinkedHashMap::new, Collectors.toList()));

        return groups.stream()
            .map(group -> {
                GroupDimensionTemplate template = ScenarioDimensionMapper.resolve(group.dimensionDefinitions(), templates, group.eventTypeL2Id());
                List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(group.dimensionSelected());
                List<ScenarioDimensionColumnRes> dimensionColumns = template.columns(selectedKeys);
                List<ScenarioBaseRow> groupScenarios = scenariosByGroup.getOrDefault(group.scenarioGroupId(), List.of());
                Map<Long, Integer> adjusted = ScenarioProbabilityAllocator.redistributeRows(groupScenarios);
                List<EventRelatedScenarioRes> scenarios = groupScenarios.stream()
                    .map(s -> EventRelatedScenarioRes.of(
                        s,
                        template.dimensions(selectedKeys, s.dimensionValues()),
                        adjusted.get(s.scenarioId()),
                        ScenarioProbabilityAllocator.ratioScale(group.apiVersion())
                    ))
                    .toList();
                return EventRelatedScenariosRes.of(group, dimensionColumns, scenarios);
            })
            .toList();
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
     * - isPermitted: 현재 사용자(고객사)의 상품 조회 권한 여부 — goodsViewableMap을 1회 조회해 row별 goodsNo로 판정
     */
    @Override
    public PaginationRes<EventRelatedRecordRes> getEventRecords(Long eventId, int page, int size) {
        PermissionRes permissions = authorizationService.getPermissions();

        Map<Integer, Boolean> goodsViewableMap = permissions.getGoodsViewableMap(authorizationService.getCompanyNoWithPriority());

        return PaginationRes.toRes(
            miEventRepository.findEventRecords(eventId, PageRequest.of(page - 1, size))
                .map(row -> EventRelatedRecordRes.from(
                    row,
                    row.getGoodsNo() != null && goodsViewableMap.getOrDefault(row.getGoodsNo(), Boolean.FALSE)))
        );
    }

    /**
     * [이벤트 설계 점유/가능 상태] (이벤트 목록 폴링)
     *  - 요청한 이벤트별로 활성 그룹 status에서 설계 상태를 파생한다 — 그룹 없음=designable, creating=설계중(점유), created/realized=설계완료.
     *  - 멀티 이벤트 전환으로 이벤트당 그룹 N건이 정식이며, 표시 상태는 기준 그룹(가장 오래된 활성 그룹) 1건에서 파생한다
     */
    @Override
    public List<EventDesignAvailabilityRes> getEventDesignAvailability(List<Long> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) return List.of();

        Map<Long, EventDesignAvailabilityRow> rowByEventId = miEventRepository.findDesignAvailability(eventIds).stream()
            .collect(Collectors.toMap(
                EventDesignAvailabilityRow::miEventId,
                Function.identity(),
                (a, b) -> {
                    // 이벤트당 그룹 N건 중 기준 그룹(가장 오래된) 선택 — 목록 쿼리의 기준 그룹 규칙과 동일
                    return a.scenarioGroupId() <= b.scenarioGroupId() ? a : b;
                }));

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

    /**
     * [이벤트 연결 시나리오 그룹 목록] (이벤트 목록의 그룹 N 펼침 패널)
     * - 그룹 최신순, 그룹별 구성 이벤트명(표시 순서)과 진행 단계를 함께 반환
     * - 실패 방치 그룹 잠정 필터: 설계중(creating)인데 최신 operation이 failed/interrupted면 제외.
     *   operation이 없는 그룹(v1 등)과 설계 완료 그룹은 필터하지 않는다. Agent의 실패 그룹 처리 규약 확정 시 교체 예정
     */
    @Override
    public List<EventLinkedScenarioGroupRes> getEventScenarioGroups(Long eventId) {

        List<EventLinkedScenarioGroupRow> rows = miEventRepository.findLinkedScenarioGroups(eventId);

        if (rows.isEmpty()) return List.of();

        // 평탄화 행(그룹 x 구성 이벤트)을 그룹 단위로 묶는다 (조회 순서 = 그룹 최신순 유지)
        Map<Long, List<EventLinkedScenarioGroupRow>> rowsByGroup = rows.stream()
            .collect(Collectors.groupingBy(EventLinkedScenarioGroupRow::scenarioGroupId, LinkedHashMap::new, Collectors.toList()));

        Set<Long> zombieGroupIds = resolveFailedCreatingGroupIds(rowsByGroup);
        Long userId = authorizationService.getUserNo().longValue();

        List<EventLinkedScenarioGroupRes> result = new ArrayList<>(rowsByGroup.size());
        for (List<EventLinkedScenarioGroupRow> groupRows : rowsByGroup.values()) {
            EventLinkedScenarioGroupRow first = groupRows.get(0);
            if (zombieGroupIds.contains(first.scenarioGroupId())) {
                continue;
            }
            List<String> events = groupRows.stream().map(EventLinkedScenarioGroupRow::memberEventName).toList();
            // mine: 설계중 그룹의 진행자가 로그인 사용자인지 (다른 사용자면 패널 카드에 잠금 배지)
            Boolean mine = userId.equals(first.requestUserId());
            result.add(EventLinkedScenarioGroupRes.of(
                first.scenarioGroupId(), first.groupTitle(), first.status(), first.stage(), mine, events));
        }
        return result;
    }

    /** 설계중(creating) 그룹 중 실패 방치(좀비) 그룹 ID 집합. (판정은 FailedCreatingGroupResolver 단일 지점) */
    private Set<Long> resolveFailedCreatingGroupIds(Map<Long, List<EventLinkedScenarioGroupRow>> rowsByGroup) {
        return failedCreatingGroupResolver.resolve(rowsByGroup.values().stream()
            .map(groupRows -> groupRows.get(0))
            .filter(row -> row.status() == ScenarioGroupStatus.CREATING)
            .map(EventLinkedScenarioGroupRow::scenarioGroupId)
            .toList());
    }

    private EventDesignAvailabilityRes toAvailability(Long eventId, EventDesignAvailabilityRow row, Map<Long, String> occupantNames) {
        if (row == null) return EventDesignAvailabilityRes.designable(eventId);
        ScenarioDesignDisplayStatus status = ScenarioDesignDisplayStatus.of(row.scenarioGroupStatus(), row.templateAvailable());
        boolean occupied = row.scenarioGroupStatus() == ScenarioGroupStatus.CREATING;
        return EventDesignAvailabilityRes.of(
            eventId, status,
            occupied ? ScenarioDesignDisplayStatus.CreatingStep.of(row.scenarioGroupStage()) : null,
            occupied,
            occupied ? row.requestUserId() : null,
            occupied ? occupantNames.get(row.requestUserId()) : null,
            row.scenarioGroupId());
    }

}

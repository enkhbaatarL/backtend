package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmd.QGoods.goods;
import static com.sk.domain.shared.entity.master.tmd.QGoodsData.goodsData;
import static com.sk.domain.shared.entity.master.tmd.QGoodsDataFileSummary.goodsDataFileSummary;
import static com.sk.domain.shared.entity.master.tmd.QMiRecord.miRecord;
import static com.sk.domain.shared.entity.master.tmd.QNewsSummary.newsSummary;
import static com.sk.domain.shared.entity.master.tmd.QProvider.provider;
import static com.sk.domain.shared.entity.master.tmm.QMiEvent.miEvent;
import static com.sk.domain.shared.entity.master.tmm.QMiEventRecordMap.miEventRecordMap;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL1.miEventTypeL1;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL2.miEventTypeL2;
import static com.sk.domain.shared.entity.master.tmm.QMiImpact.miImpact;
import static com.sk.domain.shared.entity.master.tmm.QMiImpactDriver.miImpactDriver;
import static com.sk.domain.shared.entity.master.tmm.QMiImpactResult.miImpactResult;
import static com.sk.domain.shared.entity.master.tmm.QMiInstitutionView.miInstitutionView;
import static com.sk.domain.shared.entity.master.tmm.QMiScenario.miScenario;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioDimensionTemplate.miScenarioDimensionTemplate;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioGroup.miScenarioGroup;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.utils.FullTextSearchUtil;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.shared.entity.master.tmm.QMiEvent;
import com.sk.domain.shared.entity.master.tmm.QMiScenarioGroupEvent;
import com.sk.domain.shared.entity.master.tmm.QMiScenarioGroupOperation;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.DesignOperationStatus;
import com.sk.domain.shared.enums.nextgen.DesignOperationType;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.enums.nextgen.TargetType;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDesignAvailabilityRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventLinkedScenarioGroupRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventOutlookRow;
import com.sk.domain.shared.repository.master.tmm.projection.QDriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventDesignAvailabilityRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventIndexImpactRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventLinkedScenarioGroupRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventListBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventOutlookRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

@Slf4j
@RequiredArgsConstructor
public class CustomMiEventRepositoryImpl implements CustomMiEventRepository {

    private static final List<String> SUPPORTED_INDEX_CODES = Arrays.stream(IndexName.values())
        .map(IndexName::getCode)
        .toList();

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<EventListRow> getEventsByConditions(EventListReq request) {

        Pageable pageable = PageRequest.of(request.page() - 1, request.size());

        BooleanBuilder where = buildEventWhere(request);
        OrderSpecifier<?> order = buildEventOrder(request);

        // 멀티 이벤트 전환으로 이벤트:활성 그룹은 N건이 정식이다. 목록의 시나리오 표기는 그룹 개수(count 서브쿼리)만 사용하고,
        // 그룹 N건 상세는 연결 그룹 패널 API가 담당한다.
        // (개수는 실패 방치 그룹을 제외한다 — 연결 그룹 패널 API와 동일 잠정 규칙, notFailedCreating 참조)
        QMiScenarioGroupEvent groupEventCount = new QMiScenarioGroupEvent("groupEventCount");

        List<EventListBaseRow> baseRows = queryFactory
            .select(new QEventListBaseRow(
                miEvent.miEventId,
                // note: 이벤트명은 event_title 대신 one_line_summary 표기 (요청 2026-06-08)
                miEvent.oneLineSummary,
                miEventTypeL1.level1Code,
                miEventTypeL1.level1Label,
                miEvent.isSignal,
                miEvent.isNew,
                miEvent.isScenarioSuggested,
                miEvent.scenarioSuggestReason,
                miEvent.createDt,
                miEvent.modifyDt,
                miEvent.latestRecordFirstCreated,
                JPAExpressions.select(groupEventCount.count())
                    .from(groupEventCount)
                    .where(
                        groupEventCount.miEvent.eq(miEvent),
                        groupEventCount.isDeleted.isFalse(),
                        groupEventCount.scenarioGroup.isDeleted.isFalse(),
                        notFailedCreating(groupEventCount))
            ))
            .from(miEvent)
            .innerJoin(miEvent.eventTypeL2, miEventTypeL2).on(miEventTypeL2.isDeleted.isFalse())
            .innerJoin(miEventTypeL2.eventTypeL1, miEventTypeL1).on(miEventTypeL1.isDeleted.isFalse())
            .where(where)
            .orderBy(order)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(miEvent.count())
            .from(miEvent)
            .innerJoin(miEvent.eventTypeL2, miEventTypeL2).on(miEventTypeL2.isDeleted.isFalse())
            .innerJoin(miEventTypeL2.eventTypeL1, miEventTypeL1).on(miEventTypeL1.isDeleted.isFalse())
            .where(where);

        if (baseRows.isEmpty()) {
            return PageableExecutionUtils.getPage(List.of(), pageable, countQuery::fetchOne);
        }

        List<Long> eventIds = baseRows.stream().map(EventListBaseRow::miEventId).toList();
        Map<Long, List<EventIndexImpactRow>> impactsByEventId = fetchIndexImpactsGroupedByEventId(eventIds);

        List<EventListRow> rows = baseRows.stream()
            .map(base -> new EventListRow(
                base.miEventId(),
                base.oneLineSummary(),
                base.level1Code(),
                base.level1Label(),
                base.isSignal(),
                base.isNew(),
                base.isScenarioSuggested(),
                base.scenarioSuggestReason(),
                base.createDt(),
                base.modifyDt(),
                base.latestRecordFirstCreated(),
                base.scenarioGroupCount(),
                impactsByEventId.getOrDefault(base.miEventId(), List.of())
            ))
            .toList();

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    /**
     * 이벤트에 연결된 시나리오 그룹과 각 그룹의 구성 이벤트를 평탄화 행으로 조회.
     * 이벤트 목록의 연결 그룹 펼침 패널용 — 그룹명, 진행 단계, 구성 이벤트명을 한 번에 가져온다.
     * 그룹 최신순(scenarioGroupId 내림차순), 그룹 내 구성 이벤트는 display_order 순.
     */
    @Override
    public List<EventLinkedScenarioGroupRow> findLinkedScenarioGroups(Long eventId) {
        QMiScenarioGroupEvent linkSource = new QMiScenarioGroupEvent("linkSource");
        QMiScenarioGroupEvent member = new QMiScenarioGroupEvent("member");
        QMiEvent memberEvent = new QMiEvent("memberEvent");

        return queryFactory
            .select(new QEventLinkedScenarioGroupRow(
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.title,
                miScenarioGroup.status,
                miScenarioGroup.stage,
                miScenarioGroup.requestUserId,
                member.displayOrder,
                // note: 이벤트명은 event_title 대신 one_line_summary 표기 (이벤트 도메인과 동일 기준)
                memberEvent.oneLineSummary
            ))
            .from(linkSource)
            .innerJoin(linkSource.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .innerJoin(member).on(member.scenarioGroup.eq(miScenarioGroup), member.isDeleted.isFalse())
            .innerJoin(member.miEvent, memberEvent)
            .where(
                linkSource.miEvent.miEventId.eq(eventId),
                linkSource.isDeleted.isFalse()
            )
            .orderBy(
                miScenarioGroup.scenarioGroupId.desc(),
                member.displayOrder.asc(),
                member.scenarioGroupEventId.asc()
            )
            .fetch();
    }

    /**
     * 실패 방치(좀비) 그룹 제외 조건 — 설계중(creating)인데 최신 create operation이 failed/interrupted로 끝난 그룹.
     * 좀비 판정의 쿼리 측 단일 지점 — 자바 측 단일 지점(FailedCreatingGroupResolver)과 같은 규칙이며, 규칙 변경 시 두 곳을 함께 고친다.
     * operation_id는 ULID라 사전순 최대값이 최신이다. operation이 없는 그룹(v1)은 제외되지 않는다.
     * Agent의 실패 그룹 처리 규약 확정 시 교체 예정 (잠정 규칙).
     */
    private BooleanExpression notFailedCreating(QMiScenarioGroupEvent link) {
        QMiScenarioGroupOperation failedOp = new QMiScenarioGroupOperation("failedOp");
        QMiScenarioGroupOperation latestOp = new QMiScenarioGroupOperation("latestOp");
        return link.scenarioGroup.status.ne(ScenarioGroupStatus.CREATING)
            .or(JPAExpressions.selectOne()
                .from(failedOp)
                .where(
                    failedOp.scenarioGroup.eq(link.scenarioGroup),
                    failedOp.isDeleted.isFalse(),
                    failedOp.operationType.eq(DesignOperationType.CREATE.getCode()),
                    failedOp.status.in(DesignOperationStatus.failedTerminalCodes()),
                    failedOp.operationId.eq(
                        JPAExpressions.select(latestOp.operationId.max())
                            .from(latestOp)
                            .where(
                                latestOp.scenarioGroup.eq(link.scenarioGroup),
                                latestOp.isDeleted.isFalse(),
                                latestOp.operationType.eq(DesignOperationType.CREATE.getCode()))))
                .notExists());
    }

    /**
     * 이벤트의 event_type_l2에 매칭되는 비삭제 dimension template 존재 여부 (EXISTS로 판정)
     */
    private Expression<Boolean> templateAvailable(NumberExpression<Long> eventTypeL2Id) {
        return new CaseBuilder()
            .when(JPAExpressions.selectOne()
                .from(miScenarioDimensionTemplate)
                .where(
                    miScenarioDimensionTemplate.eventTypeL2.eventTypeL2Id.eq(eventTypeL2Id),
                    miScenarioDimensionTemplate.isDeleted.isFalse())
                .exists())
            .then(true)
            .otherwise(false);
    }

    private BooleanBuilder buildEventWhere(EventListReq request) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(miEvent.isDeleted.isFalse());

        applyStatusFilter(where, request.eventStatus());
        applySuggestedFilter(where, request.suggestedOnly());
        applyTypeFilter(where, request.eventTypeL1Id());
        applyPeriodFilter(where, request);
        applyKeywordFilter(where, request.keyword());

        return where;
    }

    // signal_yn=true 강제 (소음 제외).
    private void applyStatusFilter(BooleanBuilder where, ContentStatus status) {
        switch (status) {
            case NEW -> where
                .and(miEvent.isNew.isTrue())
                .and(isSignalEvent());
            case UPDATED -> where
                .and(miEvent.isNew.isFalse())
                .and(isSignalEvent());
            case ALL -> where.and(isSignalEvent());
            default -> throw new IllegalStateException("지원하지 않는 ContentStatus 값입니다: " + status);
        }
    }

    private void applySuggestedFilter(BooleanBuilder where, Boolean suggestedOnly) {
        if (Boolean.TRUE.equals(suggestedOnly)) {
            where.and(miEvent.isScenarioSuggested.isTrue());
        }
    }

    private BooleanExpression isSignalEvent() {
        return miEvent.isSignal.isTrue();
    }

    private void applyTypeFilter(BooleanBuilder where, List<Long> eventTypeL1Ids) {
        if (eventTypeL1Ids == null || eventTypeL1Ids.isEmpty()) {
            return;
        }
        where.and(miEventTypeL1.eventTypeL1Id.in(eventTypeL1Ids));
    }

    private void applyPeriodFilter(BooleanBuilder where, EventListReq request) {
        if (request.startDate() != null) {
            where.and(miEvent.latestRecordFirstCreated.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miEvent.latestRecordFirstCreated.lt(request.endDate().plusDays(1).atStartOfDay()));
        }
    }

    private void applyKeywordFilter(BooleanBuilder where, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        // note: 이벤트명 표기를 one_line_summary 로 바꿨으므로 검색 기준도 동기화 (요청 2026-06-08)
        // note: ngram FULLTEXT BOOLEAN MODE 검색 (공백 토큰 OR, 중간 단어 부분매칭) (요청 2026-07-20)
        where.and(FullTextSearchUtil.matchBoolean(miEvent.oneLineSummary, keyword));
    }

    private OrderSpecifier<?> buildEventOrder(EventListReq request) {
        // 최신순 정렬 기준은 최신 연관 레코드 최초 생성일시
        return request.sort().isAscending()
            ? miEvent.latestRecordFirstCreated.asc()
            : miEvent.latestRecordFirstCreated.desc();
    }

    @Override
    public Optional<EventDetailRow> findEventDetail(Long eventId) {

        EventDetailBaseRow base = queryFactory
            .select(new QEventDetailBaseRow(
                miEvent.miEventId,
                // note: 이벤트명은 event_title 대신 one_line_summary 표기 (요청 2026-06-08)
                miEvent.oneLineSummary,
                miEvent.createDt,
                miEvent.modifyDt,
                miEvent.latestRecordFirstCreated,
                miEvent.oldestRecordFirstCreated,
                miEventTypeL1.level1Code,
                miEventTypeL1.level1Label,
                miEventTypeL2.level2Code,
                miEventTypeL2.level2Label,
                miEvent.isSignal,
                miEvent.isNew,
                miEvent.isScenarioSuggested,
                miEvent.scenarioSuggestReason,
                miEvent.detailAnalysis,
                miEvent.keyBulletsJson,
                miEvent.institutionViewSynthesis
            ))
            .from(miEvent)
            .innerJoin(miEvent.eventTypeL2, miEventTypeL2).on(miEventTypeL2.isDeleted.isFalse())
            .innerJoin(miEventTypeL2.eventTypeL1, miEventTypeL1).on(miEventTypeL1.isDeleted.isFalse())
            .where(miEvent.miEventId.eq(eventId), miEvent.isDeleted.isFalse())
            .fetchOne();

        if (base == null) {
            return Optional.empty();
        }

        // 인덱스 (시장영향, 가격방향) 조회
        List<EventIndexImpactRow> indexImpacts = fetchIndexImpactsGroupedByEventId(List.of(base.miEventId()))
            .getOrDefault(base.miEventId(), List.of());

        // 핵심요약 조회 (JSON 컬럼 {"bullets":[...]} 에서 파싱)
        List<String> keyBullets = JsonUtil.parseWrappedStringList(base.keyBulletsJson(), "bullets");

        // 전체 영향도 종합 요약
        String impactSummary = fetchImpactSummary(base.miEventId());

        // 출처 조회
        List<String> sources = fetchEventProviderNames(base.miEventId());

        // Driver Tree Scoreboard 조회 (인덱스 target_id 포함)
        List<DriverTagScoreboardRow> driverTags = fetchDriverTags(base.miEventId());

        // 연관 시나리오 — 매핑 테이블 경유로 이벤트에 연결된 활성 그룹 전체 조회 (그룹별 표 스택 표기)
        // eventTypeL2Id는 이 이벤트 매핑의 동결값 (v1 template 헤더 해석용, v4는 definitions가 우선)
        QMiScenarioGroupEvent groupEvent = new QMiScenarioGroupEvent("groupEvent");
        QMiScenarioGroupEvent eventCountSub = new QMiScenarioGroupEvent("eventCountSub");
        List<ScenarioGroupBaseRow> relatedScenarioGroups = queryFactory
            .select(new QScenarioGroupBaseRow(
                miScenarioGroup.scenarioGroupId,
                groupEvent.eventTypeL2.eventTypeL2Id,
                miScenarioGroup.title,
                miScenarioGroup.summary,
                miScenarioGroup.scenarioCount,
                JPAExpressions.select(eventCountSub.count())
                    .from(eventCountSub)
                    .where(
                        eventCountSub.scenarioGroup.eq(miScenarioGroup),
                        eventCountSub.isDeleted.isFalse()),
                miScenarioGroup.createDt,
                miScenarioGroup.modifyDt,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.dimensionDefinitions,
                miScenarioGroup.status,
                miScenarioGroup.stage,
                miScenarioGroup.requestUserId,
                // 분기항목 정의 원천 판별용 (v4는 dimension_definitions, v1은 event_type_l2 template)
                miScenarioGroup.apiVersion
            ))
            .from(groupEvent)
            .innerJoin(groupEvent.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .where(
                groupEvent.miEvent.miEventId.eq(eventId),
                groupEvent.isDeleted.isFalse()
            )
            .orderBy(miScenarioGroup.scenarioGroupId.asc())
            .fetch();

        List<Long> relatedGroupIds = relatedScenarioGroups.stream()
            .map(ScenarioGroupBaseRow::scenarioGroupId)
            .toList();
        List<ScenarioBaseRow> relatedScenarios = relatedGroupIds.isEmpty()
            ? List.of()
            : fetchRelatedScenarios(relatedGroupIds);

        return Optional.of(EventDetailRow.of(
            base,
            impactSummary,
            indexImpacts,
            keyBullets,
            sources,
            driverTags,
            relatedScenarioGroups,
            relatedScenarios
        ));
    }

    private List<ScenarioBaseRow> fetchRelatedScenarios(List<Long> scenarioGroupIds) {
        return queryFactory
            .select(new QScenarioBaseRow(
                miScenario.scenarioGroup.scenarioGroupId,
                miScenario.scenarioId,
                miScenario.title,
                miScenario.isBaseline,
                miScenario.probabilityScore,
                miScenario.status,
                miScenario.dimensionValues,
                miScenario.displayOrder,
                miScenario.scenarioRole
            ))
            .from(miScenario)
            .where(
                miScenario.scenarioGroup.scenarioGroupId.in(scenarioGroupIds),
                miScenario.isDeleted.isFalse()
            )
            .orderBy(
                miScenario.scenarioGroup.scenarioGroupId.asc(),
                miScenario.displayOrder.asc(),
                miScenario.scenarioId.asc()
            )
            .fetch();
    }

    // note: 이벤트 전체 영향도 종합 요약 (tmm_mi_impact_m.impact_summary). 이벤트당 1건.
    private String fetchImpactSummary(Long eventId) {
        return queryFactory
            .select(miImpact.impactSummary)
            .from(miImpact)
            .where(
                miImpact.miEvent.miEventId.eq(eventId),
                miImpact.isDeleted.isFalse()
            )
            .orderBy(miImpact.modifyDt.desc())
            .fetchFirst();
    }

    @Override
    public Page<EventOutlookRow> findEventOutlooks(Long eventId, int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<EventOutlookRow> rows = queryFactory
            .select(new QEventOutlookRow(
                miInstitutionView.viewDate,
                miInstitutionView.institutionName,
                miInstitutionView.viewSummary
            ))
            .from(miInstitutionView)
            .where(
                miInstitutionView.miEventId.eq(eventId),
                miInstitutionView.isDeleted.isFalse()
            )
            .orderBy(miInstitutionView.viewDate.desc(), miInstitutionView.institutionViewId.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(miInstitutionView.count())
            .from(miInstitutionView)
            .where(
                miInstitutionView.miEventId.eq(eventId),
                miInstitutionView.isDeleted.isFalse()
            );

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    // note: 이벤트의 출처(provider name) - record_type 분기 따라 두 경로
    //  - NEWS: tmd_mi_record -> tmd_nws_smry -> tmd_gdsm (gds_id) -> tmd_prvdm
    //  - 그 외: tmd_mi_record -> tmd_gdsdatam_file_smry (source_obj_id=gds_data_smry_no) -> tmd_gdsdatam -> tmd_gdsm (gds_no) -> tmd_prvdm
    //    (Record 파일별 분리 모델: 비-NEWS source_obj_id는 파일요약 PK를 가리킴)
    private List<String> fetchEventProviderNames(Long eventId) {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(fetchNewsProviderNames(eventId));
        names.addAll(fetchNonNewsProviderNames(eventId));
        return new ArrayList<>(names);
    }

    private List<String> fetchNewsProviderNames(Long eventId) {
        return queryFactory
            .select(provider.providerNm).distinct()
            .from(miEventRecordMap)
            .innerJoin(miEventRecordMap.miRecord, miRecord)
                .on(miRecord.recordType.eq("NEWS")
                    .and(miRecord.isDeleted.isFalse()))
            .innerJoin(newsSummary)
                .on(newsSummary.newsSummaryId.eq(miRecord.sourceObjId)
                    .and(newsSummary.isDeleted.isFalse()))
            .innerJoin(goods)
                .on(goods.goodsId.eq(newsSummary.goodsId)
                    .and(goods.isDeleted.isFalse()))
            .innerJoin(provider).on(provider.eq(goods.provider)
                    .and(provider.isDeleted.isFalse())
                    .and(provider.isUsed.isTrue()))
            .where(
                miEventRecordMap.miEvent.miEventId.eq(eventId),
                miEventRecordMap.isDeleted.isFalse()
            )
            .fetch();
    }

    private List<String> fetchNonNewsProviderNames(Long eventId) {
        return queryFactory
            .select(provider.providerNm).distinct()
            .from(miEventRecordMap)
            .innerJoin(miEventRecordMap.miRecord, miRecord)
                .on(miRecord.recordType.ne("NEWS")
                    .and(miRecord.isDeleted.isFalse()))
            .innerJoin(goodsDataFileSummary)
                .on(goodsDataFileSummary.goodsDataSummaryNo.eq(miRecord.sourceObjId.castToNum(Integer.class))
                    .and(goodsDataFileSummary.isDeleted.isFalse()))
            .innerJoin(goodsDataFileSummary.goodsData, goodsData).on(goodsData.isDeleted.isFalse())
            .innerJoin(goods).on(goods.eq(goodsData.goods)
                    .and(goods.isDeleted.isFalse()))
            .innerJoin(provider).on(provider.eq(goods.provider)
                    .and(provider.isDeleted.isFalse())
                    .and(provider.isUsed.isTrue()))
            .where(
                miEventRecordMap.miEvent.miEventId.eq(eventId),
                miEventRecordMap.isDeleted.isFalse()
            )
            .fetch();
    }

    // note: Event -> MiImpact -> MiImpactResult -> MiImpactDriver 경로로 Driver Tree Scoreboard 데이터 조회
    private List<DriverTagScoreboardRow> fetchDriverTags(Long eventId) {
        // 인덱스 순서(JKM → TTF → HH) 정렬용 rank
        NumberExpression<Integer> targetRank = new CaseBuilder()
            .when(miImpactResult.targetId.eq(IndexName.JKM.getCode())).then(0)
            .when(miImpactResult.targetId.eq(IndexName.TTF.getCode())).then(1)
            .when(miImpactResult.targetId.eq(IndexName.HH.getCode())).then(2)
            .otherwise(Integer.MAX_VALUE);

        return queryFactory
            .select(new QDriverTagScoreboardRow(
                miImpactResult.targetId,
                miImpactDriver.lv1Id,
                miImpactDriver.lv1Label,
                miImpactDriver.lv2Id,
                miImpactDriver.lv2Label,
                miImpactDriver.lv3Id,
                miImpactDriver.lv3Label,
                miImpactDriver.impactShort,
                miImpactDriver.impactMid,
                miImpactDriver.impactLong,
                miImpactDriver.driverReason
            ))
            .from(miImpactDriver)
            .innerJoin(miImpactDriver.impactResult, miImpactResult).on(miImpactResult.isDeleted.isFalse())
            .innerJoin(miImpactResult.impact, miImpact).on(miImpact.isDeleted.isFalse())
            .where(
                miImpactDriver.isDeleted.isFalse(),
                miImpact.miEvent.miEventId.eq(eventId),
                miImpactResult.targetType.eq(TargetType.PRICE.getCode()),
                miImpactResult.targetId.in(SUPPORTED_INDEX_CODES)
            )
            .orderBy(targetRank.asc(), miImpactDriver.matchScore.desc())
            .fetch();
    }

    // note: Event -> MiImpact -> MiImpactResult 경로. 인덱스별 단기/중기/장기 영향도(완료된 분석) 조회.
    private Map<Long, List<EventIndexImpactRow>> fetchIndexImpactsGroupedByEventId(List<Long> eventIds) {

        List<EventIndexImpactRow> rows = queryFactory
            .select(new QEventIndexImpactRow(
                miImpact.miEvent.miEventId,
                miImpactResult.targetId,
                miImpactResult.impactShort,
                miImpactResult.impactMid,
                miImpactResult.impactLong
            ))
            .from(miImpactResult)
            .innerJoin(miImpactResult.impact, miImpact).on(miImpact.isDeleted.isFalse())
            .where(
                miImpactResult.isDeleted.isFalse(),
                miImpactResult.completed.isTrue(), // note: 참조쿼리 참고
                miImpact.miEvent.miEventId.in(eventIds),
                miImpactResult.targetType.eq(TargetType.PRICE.getCode()),
                miImpactResult.targetId.in(SUPPORTED_INDEX_CODES)
            )
            .fetch();

        // 인덱스 영향도 JKM -> TTF -> HH (IndexName 순서)로 정렬해 그룹화
        return rows.stream()
            .sorted(Comparator.comparingInt(row -> SUPPORTED_INDEX_CODES.indexOf(row.targetId())))
            .collect(Collectors.groupingBy(EventIndexImpactRow::miEventId));
    }

}

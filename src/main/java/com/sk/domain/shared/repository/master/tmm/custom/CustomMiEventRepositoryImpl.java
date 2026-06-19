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
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioGroup.miScenarioGroup;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.EventListReq;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.TargetType;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDesignAvailabilityRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventOutlookRow;
import com.sk.domain.shared.repository.master.tmm.projection.QDriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventDesignAvailabilityRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventIndexImpactRow;
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
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.status,
                miScenarioGroup.requestUserId
            ))
            .from(miEvent)
            .innerJoin(miEvent.eventTypeL2, miEventTypeL2).on(miEventTypeL2.isDeleted.isFalse())
            .innerJoin(miEventTypeL2.eventTypeL1, miEventTypeL1).on(miEventTypeL1.isDeleted.isFalse())
            .leftJoin(miScenarioGroup).on(miScenarioGroup.miEvent.eq(miEvent), miScenarioGroup.isDeleted.isFalse())
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
                base.scenarioGroupId(),
                base.scenarioGroupStatus(),
                base.requestUserId(),
                impactsByEventId.getOrDefault(base.miEventId(), List.of())
            ))
            .toList();

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    /**
     * 이벤트별 설계 점유/가능 상태 조회.
     */
    @Override
    public List<EventDesignAvailabilityRow> findDesignAvailability(List<Long> eventIds) {
        return queryFactory
            .select(new QEventDesignAvailabilityRow(
                miEvent.miEventId,
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.status,
                miScenarioGroup.requestUserId
            ))
            .from(miEvent)
            .leftJoin(miScenarioGroup).on(miScenarioGroup.miEvent.eq(miEvent), miScenarioGroup.isDeleted.isFalse())
            .where(miEvent.isDeleted.isFalse(), miEvent.miEventId.in(eventIds))
            .fetch();
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
        where.and(miEvent.oneLineSummary.contains(keyword.trim()));
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
                miEventTypeL1.level1Code,
                miEventTypeL1.level1Label,
                miEventTypeL2.level2Code,
                miEventTypeL2.level2Label,
                miEvent.isSignal,
                miEvent.isNew,
                miEvent.isScenarioSuggested,
                miEvent.scenarioSuggestReason,
                miEvent.detailAnalysis,
                miEvent.keyBulletsJson
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

        // 연관 시나리오
        ScenarioGroupBaseRow relatedScenarioGroup = queryFactory
            .select(new QScenarioGroupBaseRow(
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.eventTypeL2.eventTypeL2Id,
                miScenarioGroup.title,
                miScenarioGroup.summary,
                miScenarioGroup.scenarioCount,
                miScenarioGroup.createDt,
                miScenarioGroup.modifyDt,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.status
            ))
            .from(miScenarioGroup)
            .where(
                miScenarioGroup.miEvent.miEventId.eq(eventId),
                miScenarioGroup.isDeleted.isFalse()
            )
            .fetchFirst();
        List<ScenarioBaseRow> relatedScenarios = relatedScenarioGroup == null
            ? List.of()
            : fetchRelatedScenarios(relatedScenarioGroup.scenarioGroupId());

        return Optional.of(new EventDetailRow(
            base.miEventId(),
            base.eventTitle(),
            base.createDt(),
            base.modifyDt(),
            base.latestRecordFirstCreated(),
            base.level1Code(),
            base.level1Label(),
            base.level2Code(),
            base.level2Label(),
            base.isSignal(),
            base.isNew(),
            base.isScenarioSuggested(),
            base.scenarioSuggestReason(),
            base.detailAnalysis(),
            impactSummary,
            indexImpacts,
            keyBullets,
            sources,
            driverTags,
            relatedScenarioGroup,
            relatedScenarios
        ));
    }

    private List<ScenarioBaseRow> fetchRelatedScenarios(Long scenarioGroupId) {
        return queryFactory
            .select(new QScenarioBaseRow(
                miScenario.scenarioGroup.scenarioGroupId,
                miScenario.scenarioId,
                miScenario.title,
                miScenario.isBaseline,
                miScenario.probabilityScore,
                miScenario.status,
                miScenario.dimensionValues,
                miScenario.displayOrder
            ))
            .from(miScenario)
            .where(
                miScenario.scenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miScenario.isDeleted.isFalse()
            )
            .orderBy(
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
            .innerJoin(miEventRecordMap.miRecord, miRecord).on(
                miRecord.recordType.eq("NEWS")
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
            .innerJoin(miEventRecordMap.miRecord, miRecord).on(
                miRecord.recordType.ne("NEWS")
                    .and(miRecord.isDeleted.isFalse()))
            .innerJoin(goodsDataFileSummary)
                .on(goodsDataFileSummary.goodsDataSummaryNo.castToNum(Long.class).eq(miRecord.sourceObjId)
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

package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmd.QGoods.goods;
import static com.sk.domain.shared.entity.master.tmd.QGoodsData.goodsData;
import static com.sk.domain.shared.entity.master.tmd.QMiRecord.miRecord;
import static com.sk.domain.shared.entity.master.tmd.QNewsSummary.newsSummary;
import static com.sk.domain.shared.entity.master.tmd.QProvider.provider;
import static com.sk.domain.shared.entity.master.tmm.QDriverTag.driverTag;
import static com.sk.domain.shared.entity.master.tmm.QImpact.impact;
import static com.sk.domain.shared.entity.master.tmm.QImpactResult.impactResult;
import static com.sk.domain.shared.entity.master.tmm.QMiEvent.miEvent;
import static com.sk.domain.shared.entity.master.tmm.QMiEventRecordMap.miEventRecordMap;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL1.miEventTypeL1;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL2.miEventTypeL2;
import static com.sk.domain.shared.entity.master.tmm.QQualitativeImpact.qualitativeImpact;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.IndexName;
import com.sk.domain.shared.enums.scenarioworkbench.TargetType;
import com.sk.domain.shared.repository.master.tmm.projection.*;

import java.util.ArrayList;
import java.util.Arrays;
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
                miEvent.miEventId, miEvent.eventTitle,
                miEventTypeL1.level1Code, miEventTypeL1.level1Label,
                miEvent.signalYn, miEvent.createDt, miEvent.modifyDt
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
                base.eventTitle(),
                base.level1Code(),
                base.level1Label(),
                base.signalYn(),
                base.createDt(),
                base.modifyDt(),
                impactsByEventId.getOrDefault(base.miEventId(), List.of())
            ))
            .toList();

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder buildEventWhere(EventListReq request) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(miEvent.isDeleted.isFalse());

        applyStatusFilter(where, request.eventStatus());
        applyTypeFilter(where, request.eventTypeL1Id());
        applyPeriodFilter(where, request);
        applyKeywordFilter(where, request.keyword());

        return where;
    }

    // todo: 변경 가능성 있음. https://skmihq.slack.com/archives/C0AR16F8QQG/p1777365927701349
    private void applyStatusFilter(BooleanBuilder where, ContentStatus status) {
        switch (status) {
            case NEW -> where
                .and(miEvent.createDt.eq(miEvent.modifyDt))
                .and(isSignalEvent());
            case UPDATED -> where
                .and(miEvent.createDt.ne(miEvent.modifyDt))
                .and(isSignalEvent());
            case ALL -> { /* no filter */ }
        }
    }

    private BooleanExpression isSignalEvent() {
        return miEvent.signalYn.isTrue();
    }

    private void applyTypeFilter(BooleanBuilder where, List<Long> eventTypeL1Ids) {
        if (eventTypeL1Ids == null || eventTypeL1Ids.isEmpty()) {
            return;
        }
        where.and(miEventTypeL1.eventTypeL1Id.in(eventTypeL1Ids));
    }

    private void applyPeriodFilter(BooleanBuilder where, EventListReq request) {
        if (request.startDate() != null) {
            where.and(miEvent.modifyDt.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miEvent.modifyDt.lt(request.endDate().plusDays(1).atStartOfDay()));
        }
    }

    private void applyKeywordFilter(BooleanBuilder where, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        where.and(miEvent.eventTitle.contains(keyword.trim()));
    }

    private OrderSpecifier<?> buildEventOrder(EventListReq request) {
        return request.sort().isAscending() ? miEvent.modifyDt.asc() : miEvent.modifyDt.desc();
    }

    @Override
    public Optional<EventDetailRow> findEventDetail(Long eventId) {

        EventDetailBaseRow base = queryFactory
            .select(new QEventDetailBaseRow(
                miEvent.miEventId, miEvent.eventTitle,
                miEvent.createDt, miEvent.modifyDt,
                miEventTypeL1.level1Code, miEventTypeL1.level1Label,
                miEventTypeL2.level2Code, miEventTypeL2.level2Label,
                miEvent.signalYn, miEvent.eventSummary, miEvent.keyBulletsJson
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

        // 핵심요약 조회 (JSON 컬럼에서 파싱)
        List<String> keyBullets = JsonUtil.parseJsonStringList(base.keyBulletsJson());

        // 영향도 분석 조회 (JKM + TTF + HH 합산)
        String impactAnalysisText = fetchImpactAnalysisText(base.miEventId());

        // 출처 조회
        List<String> sources = fetchEventProviderNames(base.miEventId());

        // Driver Tree Scoreboard 조회
        List<DriverTagScoreboardRow> driverTags = fetchDriverTags(base.miEventId());

        // todo 연관 시나리오

        return Optional.of(new EventDetailRow(
            base.miEventId(),
            base.eventTitle(),
            base.createDt(),
            base.modifyDt(),
            base.level1Code(),
            base.level1Label(),
            base.level2Code(),
            base.level2Label(),
            base.signalYn(),
            base.eventSummary(),
            impactAnalysisText,
            indexImpacts,
            keyBullets,
            sources,
            driverTags
        ));
    }

    // note: tmm_impactresultm.impact_assessment_reason을 인덱스별(JKM -> TTF -> HH)로 조회하여 합산.
    private String fetchImpactAnalysisText(Long eventId) {

        OrderSpecifier<Integer> indexOrder = new CaseBuilder()
            .when(impactResult.targetId.eq(IndexName.JKM.getCode())).then(1)
            .when(impactResult.targetId.eq(IndexName.TTF.getCode())).then(2)
            .when(impactResult.targetId.eq(IndexName.HH.getCode())).then(3)
            .otherwise(4)
            .asc();

        List<String> reasons = queryFactory
            .select(impactResult.impactAssessmentReason)
            .from(impact)
            .innerJoin(impactResult).on(impactResult.impact.eq(impact)
                .and(impactResult.isDeleted.isFalse()))
            .where(
                impact.isDeleted.isFalse(),
                impact.miEvent.miEventId.eq(eventId),
                impactResult.targetType.eq(TargetType.PRICE.getCode()),
                impactResult.targetId.in(SUPPORTED_INDEX_CODES),
                impactResult.impactAssessmentReason.isNotNull()
            )
            .orderBy(indexOrder)
            .fetch();

        return reasons.isEmpty() ? null : String.join(" ", reasons);
    }

    // note: 이벤트의 출처(provider name) - record_type 분기 따라 두 경로
    //  - NEWS: tmd_mi_record -> tmd_nws_smry -> tmd_gdsm (gds_id) -> tmd_prvdm
    //  - 그 외: tmd_mi_record -> tmd_gdsdatam -> tmd_gdsm -> tmd_prvdm
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
            .innerJoin(goodsData)
                .on(goodsData.goodsDataNo.castToNum(Long.class).eq(miRecord.sourceObjId)
                    .and(goodsData.isDeleted.isFalse()))
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

    // note: Event -> Impact -> ImpactResult -> DriverTag 경로로 Driver Tree Scoreboard 데이터 조회
    private List<DriverTagScoreboardRow> fetchDriverTags(Long eventId) {
        return queryFactory
            .select(new QDriverTagScoreboardRow(
                driverTag.lv1Id,
                driverTag.lv1Label,
                driverTag.lv2Id,
                driverTag.lv2Label,
                driverTag.lv3Id,
                driverTag.lv3Label,
                driverTag.driverMagnitude,
                driverTag.direction,
                driverTag.driverReason
            ))
            .from(driverTag)
            .innerJoin(driverTag.impactResult, impactResult).on(impactResult.isDeleted.isFalse())
            .innerJoin(impactResult.impact, impact).on(impact.isDeleted.isFalse())
            .where(
                driverTag.isDeleted.isFalse(),
                impact.miEvent.miEventId.eq(eventId)
            )
            .fetch();
    }

    private Map<Long, List<EventIndexImpactRow>> fetchIndexImpactsGroupedByEventId(List<Long> eventIds) {

        List<EventIndexImpactRow> rows = queryFactory
            .select(new QEventIndexImpactRow(
                impact.miEvent.miEventId,
                impactResult.targetId,
                qualitativeImpact.magnitudeLevel,
                qualitativeImpact.direction
            ))
            .from(impact)
            .innerJoin(impactResult).on(impactResult.impact.eq(impact)
                .and(impactResult.isDeleted.isFalse()))
            .leftJoin(qualitativeImpact)
                .on(qualitativeImpact.impactResultId.eq(impactResult.impactResultId)
                    .and(qualitativeImpact.isDeleted.isFalse()))
            .where(
                impact.isDeleted.isFalse(),
                impact.miEvent.miEventId.in(eventIds),
                impactResult.targetType.eq(TargetType.PRICE.getCode()),
                impactResult.targetId.in(SUPPORTED_INDEX_CODES)
            )
            .fetch();

        return rows.stream().collect(Collectors.groupingBy(EventIndexImpactRow::miEventId));
    }

}

package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmd.QGoods.goods;
import static com.sk.domain.shared.entity.master.tmd.QGoodsData.goodsData;
import static com.sk.domain.shared.entity.master.tmd.QMiEventKeyBullet.miEventKeyBullet;
import static com.sk.domain.shared.entity.master.tmd.QMiEventSourceMap.miEventSourceMap;
import static com.sk.domain.shared.entity.master.tmd.QMiRecord.miRecord;
import static com.sk.domain.shared.entity.master.tmd.QNewsSummary.newsSummary;
import static com.sk.domain.shared.entity.master.tmd.QProvider.provider;
import static com.sk.domain.shared.entity.master.tmm.QImpact.impact;
import static com.sk.domain.shared.entity.master.tmm.QImpactResult.impactResult;
import static com.sk.domain.shared.entity.master.tmm.QMiEvent.miEvent;
import static com.sk.domain.shared.entity.master.tmm.QDriverTag.driverTag;
import static com.sk.domain.shared.entity.master.tmm.QQualitativeImpact.qualitativeImpact;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.mi.dto.request.scenarioworkbench.EventListReq;
import com.sk.domain.shared.entity.master.tmm.MiEvent;
import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import com.sk.domain.shared.enums.scenarioworkbench.IndexName;
import com.sk.domain.shared.enums.scenarioworkbench.TargetType;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import com.sk.domain.shared.repository.master.tmm.projection.QDriverTagScoreboardRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventIndexImpactRow;

import lombok.RequiredArgsConstructor;

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

        List<MiEvent> events = queryFactory
            .selectFrom(miEvent)
            .where(where)
            .orderBy(order)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(miEvent.count())
            .from(miEvent)
            .where(where);

        if (events.isEmpty()) {
            return PageableExecutionUtils.getPage(List.of(), pageable, countQuery::fetchOne);
        }

        List<Long> eventIds = events.stream().map(MiEvent::getMiEventId).toList();
        Map<Long, List<EventIndexImpactRow>> impactsByEventId = fetchIndexImpactsGroupedByEventId(eventIds);

        List<EventListRow> rows = events.stream()
            .map(e -> new EventListRow(
                e.getMiEventId(),
                e.getEventTitleKo(),
                e.getLevel1Code(),
                e.getCreateDt(),
                e.getModifyDt(),
                impactsByEventId.getOrDefault(e.getMiEventId(), List.of())
            ))
            .toList();

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder buildEventWhere(EventListReq request) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(miEvent.isDeleted.isFalse());

        applyStatusFilter(where, request.eventStatus());
        applyCategoryFilter(where, request.eventCategoryLv1());
        applyPeriodFilter(where, request);
        applyKeywordFilter(where, request.keyword());

        return where;
    }

    // todo: 변경 가능성 있음. https://skmihq.slack.com/archives/C0AR16F8QQG/p1777365927701349
    private void applyStatusFilter(BooleanBuilder where, ContentStatus status) {
        switch (status) {
            case NEW -> where.and(miEvent.createDt.eq(miEvent.modifyDt));
            case UPDATED -> where.and(miEvent.createDt.ne(miEvent.modifyDt));
            case ALL -> { /* no filter */ }
        }
    }

    private void applyCategoryFilter(BooleanBuilder where, List<EventCategoryLv1> categories) {
        if (categories.isEmpty()) {
            return;
        }
        List<String> categoryCodes = categories.stream()
            .map(EventCategoryLv1::getCode)
            .toList();
        where.and(miEvent.level1Code.in(categoryCodes));
    }

    private void applyPeriodFilter(BooleanBuilder where, EventListReq request) {
        LocalDateTime now = LocalDateTime.now();
        request.period().calcStartDateTime(now, request.startDate())
            .ifPresent(start -> where.and(miEvent.modifyDt.goe(start)));
        request.period().calcEndDateTime(request.endDate())
            .ifPresent(end -> where.and(miEvent.modifyDt.lt(end)));
    }

    private void applyKeywordFilter(BooleanBuilder where, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        where.and(miEvent.eventTitleKo.contains(keyword.trim()));
    }

    private OrderSpecifier<?> buildEventOrder(EventListReq request) {
        return request.sort().isAscending() ? miEvent.modifyDt.asc() : miEvent.modifyDt.desc();
    }

    @Override
    public Optional<EventDetailRow> findEventDetail(Long eventId) {

        MiEvent event = queryFactory
            .selectFrom(miEvent)
            .where(miEvent.miEventId.eq(eventId), miEvent.isDeleted.isFalse())
            .fetchOne();

        if (event == null) {
            return Optional.empty();
        }

        // 인덱스 (시장영향, 가격방향) 조회
        List<EventIndexImpactRow> indexImpacts = fetchIndexImpactsGroupedByEventId(List.of(event.getMiEventId()))
            .getOrDefault(event.getMiEventId(), List.of());

        // 핵심요약 조회
        List<String> keyBullets = fetchKeyBullets(event.getMiEventId());

        // 영향도 분석 조회
        String impactAnalysisText = fetchImpactAnalysisText(event.getMiEventId());

        // 출처 조회
        List<String> sources = fetchEventProviderNames(event.getMiEventId());

        // Driver Tree Scoreboard 조회
        List<DriverTagScoreboardRow> driverTags = fetchDriverTags(event.getMiEventId());

        // todo 연관 시나리오

        return Optional.of(new EventDetailRow(
            event.getMiEventId(),
            event.getEventTitleKo(),
            event.getCreateDt(),
            event.getModifyDt(),
            event.getLevel1Code(),
            event.getLevel2Code(),
            event.getDetailAnalysis(),
            impactAnalysisText,
            indexImpacts,
            keyBullets,
            sources,
            driverTags
        ));
    }

    private List<String> fetchKeyBullets(Long eventId) {
        return queryFactory
            .select(miEventKeyBullet.bulletText)
            .from(miEventKeyBullet)
            .where(miEventKeyBullet.miEventId.eq(eventId))
            .orderBy(miEventKeyBullet.bulletOrder.asc())
            .fetch();
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
            .innerJoin(impactResult)
                .on(impactResult.impactId.eq(impact.impactId)
                    .and(impactResult.isDeleted.isFalse()))
            .where(
                impact.isDeleted.isFalse(),
                impact.miEventId.eq(eventId),
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
            .from(miEventSourceMap)
            .innerJoin(miRecord)
                .on(miRecord.miRecordId.eq(miEventSourceMap.miRecordId)
                    .and(miRecord.recordType.eq("NEWS"))
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
            .where(miEventSourceMap.miEventId.eq(eventId))
            .fetch();
    }

    private List<String> fetchNonNewsProviderNames(Long eventId) {
        return queryFactory
            .select(provider.providerNm).distinct()
            .from(miEventSourceMap)
            .innerJoin(miRecord)
                .on(miRecord.miRecordId.eq(miEventSourceMap.miRecordId)
                    .and(miRecord.recordType.ne("NEWS"))
                    .and(miRecord.isDeleted.isFalse()))
            .innerJoin(goodsData)
                .on(goodsData.goodsDataNo.castToNum(Long.class).eq(miRecord.sourceObjId)
                    .and(goodsData.isDeleted.isFalse()))
            .innerJoin(goods).on(goods.eq(goodsData.goods)
                    .and(goods.isDeleted.isFalse()))
            .innerJoin(provider).on(provider.eq(goods.provider)
                    .and(provider.isDeleted.isFalse())
                    .and(provider.isUsed.isTrue()))
            .where(miEventSourceMap.miEventId.eq(eventId))
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
            .innerJoin(impactResult)
                .on(impactResult.impactResultId.eq(driverTag.impactResultId)
                    .and(impactResult.isDeleted.isFalse()))
            .innerJoin(impact)
                .on(impact.impactId.eq(impactResult.impactId)
                    .and(impact.isDeleted.isFalse()))
            .where(
                driverTag.isDeleted.isFalse(),
                impact.miEventId.eq(eventId)
            )
            .fetch();
    }

    private Map<Long, List<EventIndexImpactRow>> fetchIndexImpactsGroupedByEventId(List<Long> eventIds) {

        List<EventIndexImpactRow> rows = queryFactory
            .select(new QEventIndexImpactRow(
                impact.miEventId,
                impactResult.targetId,
                qualitativeImpact.magnitudeLevel,
                qualitativeImpact.direction
            ))
            .from(impact)
            .innerJoin(impactResult)
                .on(impactResult.impactId.eq(impact.impactId)
                    .and(impactResult.isDeleted.isFalse()))
            .leftJoin(qualitativeImpact)
                .on(qualitativeImpact.impactResultId.eq(impactResult.impactResultId)
                    .and(qualitativeImpact.isDeleted.isFalse()))
            .where(
                impact.isDeleted.isFalse(),
                impact.miEventId.in(eventIds),
                impactResult.targetType.eq(TargetType.PRICE.getCode()),
                impactResult.targetId.in(SUPPORTED_INDEX_CODES)
            )
            .fetch();

        return rows.stream().collect(Collectors.groupingBy(EventIndexImpactRow::miEventId));
    }

}

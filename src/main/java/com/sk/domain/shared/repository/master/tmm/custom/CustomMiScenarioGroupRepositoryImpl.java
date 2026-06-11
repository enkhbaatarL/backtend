package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmm.QMiEvent.miEvent;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL1.miEventTypeL1;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL2.miEventTypeL2;
import static com.sk.domain.shared.entity.master.tmm.QMiScenario.miScenario;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioDimensionTemplate.miScenarioDimensionTemplate;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioGroup.miScenarioGroup;
import static com.sk.domain.shared.entity.master.tmm.QMiSignpost.miSignpost;
import static com.sk.domain.shared.entity.master.tmm.QMiSignpostEvent.miSignpostEvent;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioDimensionTemplateRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioIndividualBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioSignpostEventRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioSignpostRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDimensionTemplateRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioIndividualBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioSignpostEventRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioSignpostRow;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

@RequiredArgsConstructor
public class CustomMiScenarioGroupRepositoryImpl implements CustomMiScenarioGroupRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ScenarioGroupBaseRow> findScenarioGroupPage(ScenarioGroupListReq request) {

        Pageable pageable = PageRequest.of(request.page() - 1, request.size());

        BooleanBuilder where = buildGroupWhere(request);
        // 최신순 정렬 기준은 그룹 수정일시
        OrderSpecifier<?> order = request.sort().isAscending()
            ? miScenarioGroup.modifyDt.asc()
            : miScenarioGroup.modifyDt.desc();

        List<ScenarioGroupBaseRow> rows = queryFactory
            .select(new QScenarioGroupBaseRow(
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.eventTypeL2.eventTypeL2Id,
                miScenarioGroup.title,
                miScenarioGroup.summary,
                miScenarioGroup.scenarioCount,
                miScenarioGroup.createDt,
                miScenarioGroup.modifyDt,
                miScenarioGroup.dimensionSelected
            ))
            .from(miScenarioGroup)
            .where(where)
            .orderBy(order, miScenarioGroup.scenarioGroupId.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(miScenarioGroup.count())
            .from(miScenarioGroup)
            .where(where);

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder buildGroupWhere(ScenarioGroupListReq request) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(miScenarioGroup.isDeleted.isFalse());

        if (request.startDate() != null) {
            where.and(miScenarioGroup.modifyDt.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miScenarioGroup.modifyDt.lt(request.endDate().plusDays(1).atStartOfDay()));
        }
        if (request.keyword() != null && !request.keyword().isBlank()) {
            where.and(miScenarioGroup.title.contains(request.keyword().trim()));
        }
        return where;
    }

    @Override
    public Page<ScenarioIndividualBaseRow> findScenarioPage(ScenarioGroupListReq request) {

        Pageable pageable = PageRequest.of(request.page() - 1, request.size());

        BooleanBuilder where = buildScenarioWhere(request);
        // 개별 모드: 최신순 정렬/기간 필터 모두 시나리오 수정일시 기준
        OrderSpecifier<?> dateOrder = request.sort().isAscending()
            ? miScenario.modifyDt.asc()
            : miScenario.modifyDt.desc();

        List<ScenarioIndividualBaseRow> rows = queryFactory
            .select(new QScenarioIndividualBaseRow(
                miScenario.scenarioId,
                miScenario.title,
                miScenario.isBaseline,
                miScenario.probabilityScore,
                miScenario.dimensionValues,
                miScenario.createDt,
                miScenario.modifyDt,
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.title,
                miScenarioGroup.createDt,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.eventTypeL2.eventTypeL2Id,
                miScenario.displayOrder
            ))
            .from(miScenario)
            .innerJoin(miScenario.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .where(where)
            .orderBy(
                dateOrder,
                miScenarioGroup.scenarioGroupId.desc(),
                miScenario.displayOrder.asc(),
                miScenario.scenarioId.asc()
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(miScenario.count())
            .from(miScenario)
            .innerJoin(miScenario.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .where(where);

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder buildScenarioWhere(ScenarioGroupListReq request) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(miScenario.isDeleted.isFalse());

        if (request.startDate() != null) {
            where.and(miScenario.modifyDt.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miScenario.modifyDt.lt(request.endDate().plusDays(1).atStartOfDay()));
        }
        // 개별 모드 키워드는 시나리오명 검색
        if (request.keyword() != null && !request.keyword().isBlank()) {
            where.and(miScenario.title.contains(request.keyword().trim()));
        }
        return where;
    }

    @Override
    public Optional<ScenarioGroupDetailBaseRow> findScenarioGroupDetail(Long scenarioGroupId) {
        ScenarioGroupDetailBaseRow row = queryFactory
            .select(new QScenarioGroupDetailBaseRow(
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.title,
                miScenarioGroup.createDt,
                miScenarioGroup.modifyDt,
                miScenarioGroup.summary,
                miScenarioGroup.scenarioCount,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.eventTypeL2.eventTypeL2Id,
                miEvent.miEventId,
                // note: 이벤트명은 event_title 대신 one_line_summary 표기 (이벤트 도메인과 동일 기준, 요청 2026-06-08)
                miEvent.oneLineSummary,
                miEventTypeL1.level1Code,
                miEventTypeL1.level1Label,
                miEvent.modifyDt
            ))
            .from(miScenarioGroup)
            .innerJoin(miScenarioGroup.miEvent, miEvent).on(miEvent.isDeleted.isFalse())
            .innerJoin(miScenarioGroup.eventTypeL2, miEventTypeL2).on(miEventTypeL2.isDeleted.isFalse())
            .innerJoin(miEventTypeL2.eventTypeL1, miEventTypeL1).on(miEventTypeL1.isDeleted.isFalse())
            .where(
                miScenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miScenarioGroup.isDeleted.isFalse()
            )
            .fetchOne();
        return Optional.ofNullable(row);
    }

    @Override
    public List<ScenarioBaseRow> findScenariosByGroupIds(List<Long> scenarioGroupIds) {
        if (scenarioGroupIds == null || scenarioGroupIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
            .select(new QScenarioBaseRow(
                miScenario.scenarioGroup.scenarioGroupId,
                miScenario.scenarioId,
                miScenario.title,
                miScenario.isBaseline,
                miScenario.probabilityScore,
                miScenario.judgmentStatusCode,
                miScenario.dimensionValues,
                miScenario.displayOrder
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

    @Override
    public Optional<ScenarioDetailBaseRow> findScenarioDetail(Long scenarioGroupId, Long scenarioId) {
        ScenarioDetailBaseRow row = queryFactory
            .select(new QScenarioDetailBaseRow(
                miScenario.scenarioId,
                miScenarioGroup.scenarioGroupId,
                miScenario.title,
                miScenario.isBaseline,
                miScenario.createDt,
                miScenario.modifyDt,
                miScenario.narrative,
                miScenario.probabilityScore,
                miScenario.dimensionValues,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.eventTypeL2.eventTypeL2Id
            ))
            .from(miScenario)
            .innerJoin(miScenario.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .where(
                miScenario.scenarioId.eq(scenarioId),
                miScenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miScenario.isDeleted.isFalse()
            )
            .fetchOne();
        return Optional.ofNullable(row);
    }

    @Override
    public List<ScenarioSignpostRow> findSignpostsByScenarioId(Long scenarioId) {
        return queryFactory
            .select(new QScenarioSignpostRow(
                miSignpost.label,
                miSignpost.probabilityDirection,
                miSignpost.probabilityImpactScore,
                miSignpost.signpostReason
            ))
            .from(miSignpost)
            .where(
                miSignpost.scenario.scenarioId.eq(scenarioId),
                miSignpost.isDeleted.isFalse()
            )
            .orderBy(
                miSignpost.displayOrder.asc(),
                miSignpost.signpostId.asc()
            )
            .fetch();
    }

    @Override
    public List<ScenarioSignpostEventRow> findSignpostEventsByScenarioId(Long scenarioId) {
        return queryFactory
            .select(new QScenarioSignpostEventRow(
                miSignpostEvent.occurredAt,
                miSignpostEvent.summary,
                miSignpostEvent.probabilityScore,
                miSignpostEvent.probabilityDelta,
                miSignpostEvent.matchingReason
            ))
            .from(miSignpostEvent)
            .innerJoin(miSignpostEvent.signpost, miSignpost).on(miSignpost.isDeleted.isFalse())
            .where(
                miSignpost.scenario.scenarioId.eq(scenarioId),
                miSignpostEvent.isDeleted.isFalse()
            )
            .orderBy(
                miSignpostEvent.occurredAt.asc().nullsLast(),
                miSignpostEvent.signpostEventId.asc()
            )
            .fetch();
    }

    @Override
    public List<MiScenario> findScenarioEntitiesByGroupId(Long scenarioGroupId) {
        return queryFactory
            .selectFrom(miScenario)
            .innerJoin(miScenario.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .where(
                miScenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miScenario.isDeleted.isFalse()
            )
            .orderBy(
                miScenario.displayOrder.asc(),
                miScenario.scenarioId.asc()
            )
            .fetch();
    }

    @Override
    public List<ScenarioDimensionTemplateRow> findTemplatesByEventTypeL2Ids(List<Long> eventTypeL2Ids) {
        if (eventTypeL2Ids == null || eventTypeL2Ids.isEmpty()) {
            return List.of();
        }
        return queryFactory
            .select(new QScenarioDimensionTemplateRow(
                miScenarioDimensionTemplate.eventTypeL2.eventTypeL2Id,
                miScenarioDimensionTemplate.templateJson
            ))
            .from(miScenarioDimensionTemplate)
            .where(
                miScenarioDimensionTemplate.eventTypeL2.eventTypeL2Id.in(eventTypeL2Ids),
                miScenarioDimensionTemplate.isDeleted.isFalse()
            )
            .fetch();
    }

}

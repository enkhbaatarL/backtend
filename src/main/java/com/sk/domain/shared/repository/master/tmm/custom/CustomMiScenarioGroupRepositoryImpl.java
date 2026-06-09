package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmm.QMiEvent.miEvent;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL1.miEventTypeL1;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL2.miEventTypeL2;
import static com.sk.domain.shared.entity.master.tmm.QMiScenario.miScenario;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioDimensionTemplate.miScenarioDimensionTemplate;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioGroup.miScenarioGroup;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioDimensionTemplateRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioIndividualBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDimensionTemplateRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioIndividualBaseRow;
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
        OrderSpecifier<?> order = request.sort().isAscending()
            ? miScenarioGroup.createDt.asc()
            : miScenarioGroup.createDt.desc();

        List<ScenarioGroupBaseRow> rows = queryFactory
            .select(new QScenarioGroupBaseRow(
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.eventTypeL2.eventTypeL2Id,
                miScenarioGroup.title,
                miScenarioGroup.summary,
                miScenarioGroup.scenarioCount,
                miScenarioGroup.createDt,
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
            where.and(miScenarioGroup.createDt.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miScenarioGroup.createDt.lt(request.endDate().plusDays(1).atStartOfDay()));
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
        // 개별 모드 정렬/기간 필터 기준은 부모 그룹 생성일시 (그룹 모드와 동일 기준)
        OrderSpecifier<?> dateOrder = request.sort().isAscending()
            ? miScenarioGroup.createDt.asc()
            : miScenarioGroup.createDt.desc();

        List<ScenarioIndividualBaseRow> rows = queryFactory
            .select(new QScenarioIndividualBaseRow(
                miScenario.scenarioId,
                miScenario.title,
                miScenario.isBaseline,
                miScenario.probabilityScore,
                miScenario.dimensionValues,
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
            where.and(miScenarioGroup.createDt.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miScenarioGroup.createDt.lt(request.endDate().plusDays(1).atStartOfDay()));
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
                miScenarioGroup.summary,
                miScenarioGroup.scenarioCount,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.eventTypeL2.eventTypeL2Id,
                miEvent.miEventId,
                miEvent.eventTitle,
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

package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmm.QMiEvent.miEvent;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL1.miEventTypeL1;
import static com.sk.domain.shared.entity.master.tmm.QMiEventTypeL2.miEventTypeL2;
import static com.sk.domain.shared.entity.master.tmm.QMiScenario.miScenario;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioDimensionTemplate.miScenarioDimensionTemplate;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioGroup.miScenarioGroup;
import static com.sk.domain.shared.entity.master.tmm.QMiScenarioGroupEvent.miScenarioGroupEvent;
import static com.sk.domain.shared.entity.master.tmm.QMiSignpost.miSignpost;
import static com.sk.domain.shared.entity.master.tmm.QMiSignpostEvent.miSignpostEvent;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tmm.QMiScenarioGroupEvent;
import com.sk.common.utils.FullTextSearchUtil;
import com.sk.domain.mi.dto.request.nextgen.ScenarioGroupListReq;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiSignpost;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioDimensionTemplateRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupBaseEventRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioGroupDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioIndividualBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioSignpostEventRow;
import com.sk.domain.shared.repository.master.tmm.projection.QScenarioSignpostRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDetailBaseRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDimensionTemplateRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseEventRow;
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

    /**
     * 그룹의 기준 이벤트 매핑 행 조인 조건 (활성 매핑 중 최소 ID = display_order 첫 이벤트).
     *
     * 멀티 이벤트 그룹은 매핑이 N행이므로, 단일 값 파생(eventTypeL2Id, base event)은 기준 행 1건으로
     * 고정해 목록 행 중복과 페이지네이션 붕괴를 막는다. v1 그룹은 backfill로 항상 1행이라 동작이 동일하다.
     */
    private BooleanExpression primaryGroupEventJoin() {
        QMiScenarioGroupEvent groupEventSub = new QMiScenarioGroupEvent("groupEventSub");
        return miScenarioGroupEvent.scenarioGroup.eq(miScenarioGroup)
            .and(miScenarioGroupEvent.scenarioGroupEventId.eq(
                JPAExpressions.select(groupEventSub.scenarioGroupEventId.min())
                    .from(groupEventSub)
                    .where(
                        groupEventSub.scenarioGroup.eq(miScenarioGroup),
                        groupEventSub.isDeleted.isFalse())));
    }

    @Override
    public Page<ScenarioGroupBaseRow> findScenarioGroupPage(ScenarioGroupListReq request) {

        Pageable pageable = PageRequest.of(request.page() - 1, request.size());

        BooleanBuilder where = buildGroupWhere(request);
        // 최신순 정렬 기준은 그룹 생성일시
        OrderSpecifier<?> order = request.sort().isAscending()
            ? miScenarioGroup.createDt.asc()
            : miScenarioGroup.createDt.desc();

        List<ScenarioGroupBaseRow> rows = queryFactory
            .select(new QScenarioGroupBaseRow(
                miScenarioGroup.scenarioGroupId,
                miScenarioGroupEvent.eventTypeL2.eventTypeL2Id,
                miScenarioGroup.title,
                miScenarioGroup.summary,
                miScenarioGroup.scenarioCount,
                miScenarioGroup.createDt,
                miScenarioGroup.modifyDt,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.dimensionDefinitions,
                miScenarioGroup.status,
                miScenarioGroup.stage,
                miScenarioGroup.requestUserId
            ))
            .from(miScenarioGroup)
            .leftJoin(miScenarioGroupEvent).on(primaryGroupEventJoin())
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
        // 설계(생성) 진행 중인 그룹은 목록에서 제외 — 생성 완료(created)/실현(realized)만 노출
        where.and(miScenarioGroup.status.ne(ScenarioGroupStatus.CREATING));

        if (request.startDate() != null) {
            where.and(miScenarioGroup.createDt.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miScenarioGroup.createDt.lt(request.endDate().plusDays(1).atStartOfDay()));
        }
        // note: ngram FULLTEXT BOOLEAN MODE 검색 (공백 토큰 OR, 중간 단어 부분매칭) (요청 2026-07-20)
        if (request.keyword() != null && !request.keyword().isBlank()) {
            where.and(FullTextSearchUtil.matchBoolean(miScenarioGroup.title, request.keyword()));
        }
        return where;
    }

    @Override
    public Page<ScenarioIndividualBaseRow> findScenarioPage(ScenarioGroupListReq request) {

        Pageable pageable = PageRequest.of(request.page() - 1, request.size());

        BooleanBuilder where = buildScenarioWhere(request);
        // 개별 모드: 최신순 정렬/기간 필터 모두 시나리오 생성일시 기준
        OrderSpecifier<?> dateOrder = request.sort().isAscending()
            ? miScenario.createDt.asc()
            : miScenario.createDt.desc();

        List<ScenarioIndividualBaseRow> rows = queryFactory
            .select(new QScenarioIndividualBaseRow(
                miScenario.scenarioId,
                miScenario.title,
                miScenario.isBaseline,
                miScenario.probabilityScore,
                miScenario.status,
                miScenario.dimensionValues,
                miScenario.createDt,
                miScenario.modifyDt,
                miScenarioGroup.scenarioGroupId,
                miScenarioGroup.title,
                miScenarioGroup.createDt,
                miScenarioGroup.dimensionSelected,
                miScenarioGroup.dimensionDefinitions,
                miScenarioGroupEvent.eventTypeL2.eventTypeL2Id,
                miScenario.displayOrder
            ))
            .from(miScenario)
            .innerJoin(miScenario.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .leftJoin(miScenarioGroupEvent).on(primaryGroupEventJoin())
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
        // 생성 진행 중인 시나리오는 목록에서 제외 — 생성 완료 이후(created/realized/discarded)만 노출
        where.and(miScenario.status.ne(ScenarioStatus.CREATING));
        // "설계중"은 그룹 라이프사이클 상태이므로 그룹 status도 함께 제외
        where.and(miScenarioGroup.status.ne(ScenarioGroupStatus.CREATING));

        if (request.startDate() != null) {
            where.and(miScenario.createDt.goe(request.startDate().atStartOfDay()));
        }
        if (request.endDate() != null) {
            where.and(miScenario.createDt.lt(request.endDate().plusDays(1).atStartOfDay()));
        }
        // note: ngram FULLTEXT BOOLEAN MODE 검색 (공백 토큰 OR, 중간 단어 부분매칭) (요청 2026-07-20)
        if (request.keyword() != null && !request.keyword().isBlank()) {
            where.and(FullTextSearchUtil.matchBoolean(miScenario.title, request.keyword()));
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
                miScenarioGroup.dimensionDefinitions,
                miEventTypeL2.eventTypeL2Id,
                miEvent.miEventId,
                // note: 이벤트명은 event_title 대신 one_line_summary 표기 (이벤트 도메인과 동일 기준, 요청 2026-06-08)
                miEvent.oneLineSummary,
                miEventTypeL1.level1Code,
                miEventTypeL1.level1Label,
                miEvent.modifyDt
            ))
            .from(miScenarioGroup)
            // base event는 기준 매핑 행(display_order 첫 이벤트) 기준 — 멀티 이벤트 그룹의 전체 목록 응답은 후속 계약 확정 후 확장
            .innerJoin(miScenarioGroupEvent).on(primaryGroupEventJoin())
            .innerJoin(miScenarioGroupEvent.miEvent, miEvent).on(miEvent.isDeleted.isFalse())
            .innerJoin(miScenarioGroupEvent.eventTypeL2, miEventTypeL2).on(miEventTypeL2.isDeleted.isFalse())
            .innerJoin(miEventTypeL2.eventTypeL1, miEventTypeL1).on(miEventTypeL1.isDeleted.isFalse())
            .where(
                miScenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miScenarioGroup.isDeleted.isFalse()
            )
            .fetchOne();
        return Optional.ofNullable(row);
    }

    /**
     * 그룹의 기준 eventTypeL2Id 조회 — 활성 매핑의 display_order 첫 이벤트에서 동결된 L2.
     * 멀티 이벤트 그룹의 L2 집합 판정 규칙(ModelInputMode)이 확정되기 전까지의 잠정 단일 값이다.
     */
    @Override
    public Optional<Long> findEventTypeL2IdByScenarioGroupId(Long scenarioGroupId) {
        Long eventTypeL2Id = queryFactory
            .select(miScenarioGroupEvent.eventTypeL2.eventTypeL2Id)
            .from(miScenarioGroupEvent)
            .where(
                miScenarioGroupEvent.scenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miScenarioGroupEvent.isDeleted.isFalse(),
                miScenarioGroupEvent.scenarioGroup.isDeleted.isFalse()
            )
            .orderBy(
                miScenarioGroupEvent.displayOrder.asc(),
                miScenarioGroupEvent.scenarioGroupEventId.asc()
            )
            .fetchFirst();
        return Optional.ofNullable(eventTypeL2Id);
    }

    /**
     * 그룹을 구성하는 이벤트 목록 (Base Event 리스트, display_order 순).
     * 멀티 이벤트 그룹의 설계 화면 상단 이벤트 리스트와 그룹 상세 baseEvents 공용.
     */
    @Override
    public List<ScenarioGroupBaseEventRow> findBaseEventsByScenarioGroupId(Long scenarioGroupId) {
        return queryFactory
            .select(new QScenarioGroupBaseEventRow(
                miEvent.miEventId,
                // note: 이벤트명은 event_title 대신 one_line_summary 표기 (이벤트 도메인과 동일 기준)
                miEvent.oneLineSummary,
                miEventTypeL1.level1Code,
                miEventTypeL1.level1Label,
                miEvent.isScenarioSuggested,
                miEvent.modifyDt
            ))
            .from(miScenarioGroupEvent)
            .innerJoin(miScenarioGroupEvent.miEvent, miEvent)
            .innerJoin(miEvent.eventTypeL2, miEventTypeL2)
            .innerJoin(miEventTypeL2.eventTypeL1, miEventTypeL1)
            .where(
                miScenarioGroupEvent.scenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miScenarioGroupEvent.isDeleted.isFalse()
            )
            .orderBy(
                miScenarioGroupEvent.displayOrder.asc(),
                miScenarioGroupEvent.scenarioGroupEventId.asc()
            )
            .fetch();
    }

    @Override
    public List<ScenarioBaseRow> findScenariosByGroupIds(List<Long> scenarioGroupIds) {
        if (scenarioGroupIds == null || scenarioGroupIds.isEmpty()) {
            return List.of();
        }
        return fetchScenarios(miScenario.scenarioGroup.scenarioGroupId.in(scenarioGroupIds));
    }

    @Override
    public List<ScenarioBaseRow> findScenariosByGroupId(Long scenarioGroupId) {
        return fetchScenarios(miScenario.scenarioGroup.scenarioGroupId.eq(scenarioGroupId));
    }

    private List<ScenarioBaseRow> fetchScenarios(BooleanExpression groupCondition) {
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
                groupCondition,
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
                miScenarioGroup.dimensionDefinitions,
                miScenarioGroupEvent.eventTypeL2.eventTypeL2Id,
                miScenario.displayOrder
            ))
            .from(miScenario)
            .innerJoin(miScenario.scenarioGroup, miScenarioGroup).on(miScenarioGroup.isDeleted.isFalse())
            .leftJoin(miScenarioGroupEvent).on(primaryGroupEventJoin())
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
                miSignpost.signpostId,
                miSignpost.label,
                miSignpost.probabilityDirection,
                miSignpost.probabilityImpactScore,
                miSignpost.strength,
                miSignpost.signpostReason,
                miSignpost.displayOrder
            ))
            .from(miSignpost)
            .where(
                miSignpost.scenario.scenarioId.eq(scenarioId),
                miSignpost.isDeleted.isFalse(),
                miSignpost.isUsed.isTrue()
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
        return scenarioEntitiesByGroupIdQuery(scenarioGroupId).fetch();
    }

    /**
     * 그룹 내 시나리오 엔티티를 배타 잠금(SELECT FOR UPDATE)으로 조회
     * 읽은 시나리오 전체를 함께 수정하는 쓰기 트랜잭션(폐기/실현 캐스케이드, 그룹 삭제) 전용.
     * readOnly 트랜잭션에서 호출하면 MySQL이 잠금을 거부(1792)하므로 조회 경로에서는 findScenarioEntitiesByGroupId를 사용한다.
     */
    @Override
    public List<MiScenario> findScenarioEntitiesByGroupIdForUpdate(Long scenarioGroupId) {
        return scenarioEntitiesByGroupIdQuery(scenarioGroupId)
            .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
            .fetch();
    }

    private JPAQuery<MiScenario> scenarioEntitiesByGroupIdQuery(Long scenarioGroupId) {
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
            );
    }

    @Override
    public List<MiSignpost> findSignpostEntitiesByGroupId(Long scenarioGroupId) {
        return queryFactory
            .selectFrom(miSignpost)
            .innerJoin(miSignpost.scenario, miScenario)
            .where(
                miScenario.scenarioGroup.scenarioGroupId.eq(scenarioGroupId),
                miSignpost.isDeleted.isFalse()
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

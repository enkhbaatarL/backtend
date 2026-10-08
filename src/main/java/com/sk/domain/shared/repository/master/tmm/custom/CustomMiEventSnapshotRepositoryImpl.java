package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmm.QMiEventSnapshot.miEventSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tmm.MiEventSnapshot;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotListRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventSnapshotListRow;

import lombok.RequiredArgsConstructor;

/** [MCP Snapshot] Snapshot 목록·최신·시점 조회 (QueryDSL). del_yn='N' 만, event_del_yn 으로는 거르지 않음. */
@RequiredArgsConstructor
public class CustomMiEventSnapshotRepositoryImpl implements CustomMiEventSnapshotRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<EventSnapshotListRow> findEventSnapshots(Long miEventId, LocalDateTime createdFromUtc, LocalDateTime createdToUtc,
                                                         String updateTriggerCd, String createdBy, int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        BooleanExpression[] conditions = {
            eventSnapshotOf(miEventId),
            createdFromUtc == null ? null : miEventSnapshot.createDt.goe(createdFromUtc),
            createdToUtc == null ? null : miEventSnapshot.createDt.lt(createdToUtc),
            StringUtils.isBlank(updateTriggerCd) ? null : miEventSnapshot.updateTriggerCd.eq(updateTriggerCd),
            // 생성자 표기가 'system'/'SYSTEM' 으로 섞일 수 있어 대소문자 무시 (N4)
            StringUtils.isBlank(createdBy) ? null : miEventSnapshot.createUserId.equalsIgnoreCase(createdBy)
        };

        List<EventSnapshotListRow> rows = queryFactory
            .select(new QEventSnapshotListRow(
                miEventSnapshot.miEventSnapshotId,
                miEventSnapshot.miEventId,
                miEventSnapshot.snapshotVersionNo,
                miEventSnapshot.previousMiEventSnapshotId,
                miEventSnapshot.createDt,
                miEventSnapshot.createUserId,
                miEventSnapshot.updateTriggerCd,
                miEventSnapshot.updateReason,
                miEventSnapshot.changedFieldsJson,
                miEventSnapshot.eventTitle,
                miEventSnapshot.oneLineSummary,
                miEventSnapshot.eventModifyDt
            ))
            .from(miEventSnapshot)
            .where(conditions)
            .orderBy(versionOrder())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(miEventSnapshot.count())
            .from(miEventSnapshot)
            .where(conditions);

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

    @Override
    public Optional<MiEventSnapshot> findLatestEventSnapshot(Long miEventId) {
        return Optional.ofNullable(queryFactory
            .selectFrom(miEventSnapshot)
            .where(eventSnapshotOf(miEventId))
            .orderBy(versionOrder())
            .limit(1)
            .fetchOne());
    }

    @Override
    public Optional<MiEventSnapshot> findEventSnapshotAsOf(Long miEventId, LocalDateTime asOfUtc) {
        return Optional.ofNullable(queryFactory
            .selectFrom(miEventSnapshot)
            .where(
                eventSnapshotOf(miEventId),
                miEventSnapshot.createDt.loe(asOfUtc)
            )
            .orderBy(
                miEventSnapshot.createDt.desc(),
                miEventSnapshot.snapshotVersionNo.desc().nullsLast(),
                miEventSnapshot.miEventSnapshotId.desc()
            )
            .limit(1)
            .fetchOne());
    }

    // 행 논리 삭제만 거른다 (D-8). 당시 Event 삭제 여부(isEventDeleted)로는 거르지 않는다.
    private BooleanExpression eventSnapshotOf(Long miEventId) {
        return miEventSnapshot.miEventId.eq(miEventId)
            .and(miEventSnapshot.isDeleted.isFalse());
    }

    // 버전 번호가 NULL 인 과거 행(N3)은 뒤로 보내고 저장 시각·ID 로 보조 정렬한다.
    private static OrderSpecifier<?>[] versionOrder() {
        return new OrderSpecifier<?>[] {
            miEventSnapshot.snapshotVersionNo.desc().nullsLast(),
            miEventSnapshot.createDt.desc(),
            miEventSnapshot.miEventSnapshotId.desc()
        };
    }

}

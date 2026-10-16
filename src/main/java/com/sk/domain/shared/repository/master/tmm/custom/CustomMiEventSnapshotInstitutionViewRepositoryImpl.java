package com.sk.domain.shared.repository.master.tmm.custom;

import static com.sk.domain.shared.entity.master.tmm.QMiEventSnapshotInstitutionView.miEventSnapshotInstitutionView;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotOutlookRow;
import com.sk.domain.shared.repository.master.tmm.projection.QEventSnapshotOutlookRow;

import lombok.RequiredArgsConstructor;

/** [MCP Snapshot] Snapshot 기관 전망 사본 조회. del_yn='N' 만, view_date DESC·institution_view_id DESC. */
@RequiredArgsConstructor
public class CustomMiEventSnapshotInstitutionViewRepositoryImpl implements CustomMiEventSnapshotInstitutionViewRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<EventSnapshotOutlookRow> findEventSnapshotOutlooks(Long miEventSnapshotId, String institutionName,
                                                                   LocalDate viewDateFrom, LocalDate viewDateTo, int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);
        BooleanExpression[] conditions = {
            miEventSnapshotInstitutionView.miEventSnapshotId.eq(miEventSnapshotId),
            miEventSnapshotInstitutionView.isDeleted.isFalse(),
            StringUtils.isBlank(institutionName) ? null : miEventSnapshotInstitutionView.institutionName.contains(institutionName),
            // view_date 는 DATE 라 양끝 포함
            viewDateFrom == null ? null : miEventSnapshotInstitutionView.viewDate.goe(viewDateFrom),
            viewDateTo == null ? null : miEventSnapshotInstitutionView.viewDate.loe(viewDateTo)
        };

        List<EventSnapshotOutlookRow> rows = queryFactory
            .select(new QEventSnapshotOutlookRow(
                miEventSnapshotInstitutionView.institutionViewId,
                miEventSnapshotInstitutionView.miRecordId,
                miEventSnapshotInstitutionView.institutionName,
                miEventSnapshotInstitutionView.viewDate,
                miEventSnapshotInstitutionView.viewSummary,
                miEventSnapshotInstitutionView.sourceCreateDt,
                miEventSnapshotInstitutionView.sourceModifyDt
            ))
            .from(miEventSnapshotInstitutionView)
            .where(conditions)
            .orderBy(miEventSnapshotInstitutionView.viewDate.desc(), miEventSnapshotInstitutionView.institutionViewId.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(miEventSnapshotInstitutionView.count())
            .from(miEventSnapshotInstitutionView)
            .where(conditions);

        return PageableExecutionUtils.getPage(rows, pageable, countQuery::fetchOne);
    }

}

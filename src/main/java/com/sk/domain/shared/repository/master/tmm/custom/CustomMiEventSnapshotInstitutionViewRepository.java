package com.sk.domain.shared.repository.master.tmm.custom;

import java.time.LocalDate;

import org.springframework.data.domain.Page;

import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotOutlookRow;

/** [MCP Snapshot] Snapshot 기관 전망 사본 조회 (QueryDSL). */
public interface CustomMiEventSnapshotInstitutionViewRepository {

    /**
     * Snapshot 저장 당시 기관 전망 사본. 정렬: view_date DESC, institution_view_id DESC.
     *
     * @param institutionName 기관명 부분 일치, null 이면 조건 없음
     * @param viewDateFrom    view_date 하한 (포함), null 이면 조건 없음
     * @param viewDateTo      view_date 상한 (포함), null 이면 조건 없음
     */
    Page<EventSnapshotOutlookRow> findEventSnapshotOutlooks(Long miEventSnapshotId, String institutionName,
                                                            LocalDate viewDateFrom, LocalDate viewDateTo, int page, int size);

}

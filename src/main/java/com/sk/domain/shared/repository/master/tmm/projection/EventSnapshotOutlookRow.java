package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [MCP Snapshot] Snapshot 기관 전망 사본 행. 이력 행 자체의 crt_dtm/crt_usr_logn_id 는 select 하지 않는다.
 */
public record EventSnapshotOutlookRow(

    Long institutionViewId,
    Long miRecordId,
    String institutionName,
    LocalDate viewDate,
    String viewSummary,
    LocalDateTime sourceCreateDt,
    LocalDateTime sourceModifyDt

) {

    @QueryProjection
    public EventSnapshotOutlookRow {
    }

}

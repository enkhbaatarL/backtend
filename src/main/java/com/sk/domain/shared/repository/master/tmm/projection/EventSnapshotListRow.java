package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;

/**
 * [MCP Snapshot] Snapshot 버전 목록 행. 목록 응답에 넣지 않는 LONGTEXT·JSON 본문(detail_analysis 등)은 select 하지 않는다.
 * 시각은 DB 저장값(UTC) 그대로이며 응답 변환은 DTO 에서 한다.
 */
public record EventSnapshotListRow(

    Long miEventSnapshotId,
    Long miEventId,
    Long snapshotVersionNo,
    Long previousMiEventSnapshotId,
    LocalDateTime createDt,
    String createUserId,
    String updateTriggerCd,
    String updateReason,
    String changedFieldsJson,
    String oneLineSummary,
    LocalDateTime eventModifyDt

) {

    @QueryProjection
    public EventSnapshotListRow {
    }

}

package com.sk.domain.mi.dto.response.nextgen.snapshot;

import com.sk.common.utils.DateUtil;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotOutlookRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [MCP Snapshot] Snapshot 저장 당시 기관 전망 사본.
 */
public record EventSnapshotOutlookRes(

    @Schema(description = "원본 기관 전망 ID. institution_view_id", example = "321")
    Long institutionViewId,

    @Schema(description = "전망 근거 Record ID. mi_record_id", example = "50123")
    Long miRecordId,

    @Schema(description = "기관명. institution_name", example = "Goldman Sachs")
    String institutionName,

    @Schema(description = "전망 발행일. view_date", example = "2026-07-20")
    LocalDate viewDate,

    @Schema(description = "당시 전망 내용. view_summary")
    String outlookSummary,

    @Schema(description = "복사 당시 원본 전망 행의 DB 생성 시각 (KST). source_crt_dtm, 응답 시 KST 변환. 발행일(viewDate)과 다른 의미")
    LocalDateTime sourceCreatedAt,

    @Schema(description = "복사 당시 원본 전망 행의 DB 수정 시각 (KST). source_mod_dtm, 응답 시 KST 변환. 발행일(viewDate)과 다른 의미")
    LocalDateTime sourceModifiedAt

) {

    public static EventSnapshotOutlookRes from(EventSnapshotOutlookRow row) {
        return new EventSnapshotOutlookRes(
            row.institutionViewId(),
            row.miRecordId(),
            row.institutionName(),
            row.viewDate(),
            row.viewSummary(),
            DateUtil.utcToKst(row.sourceCreateDt()),
            DateUtil.utcToKst(row.sourceModifyDt()));
    }

}

package com.sk.domain.mi.dto.response.nextgen.snapshot;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.shared.enums.nextgen.EventSnapshotUpdateTrigger;
import com.sk.domain.shared.repository.master.tmm.projection.EventSnapshotListRow;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [MCP Snapshot] Snapshot 버전 목록 항목. 긴 본문(상세분석 등)은 넣지 않는다.
 */
public record EventSnapshotSummaryRes(

    @Schema(description = "Snapshot ID. mi_event_snapshot_id", example = "12")
    Long snapshotId,

    @Schema(description = "원본 Event ID. mi_event_id", example = "1269")
    Long eventId,

    @Schema(description = "Event 내부 버전 번호. snapshot_version_no, 과거 데이터는 null 일 수 있음", example = "4", nullable = true)
    Long versionNo,

    @Schema(description = "직전 Snapshot ID. previous_mi_event_snapshot_id, null 이면 이전 저장 이력 없음", example = "11", nullable = true)
    Long previousSnapshotId,

    @Schema(description = "Snapshot 저장 일시 (KST). crt_dtm, 응답 시 KST 변환", example = "2026-09-25 17:26:06")
    LocalDateTime createdAt,

    @Schema(description = "Snapshot 생성 주체 ID. crt_usr_logn_id, 파이프라인은 system", example = "system")
    String createdBy,

    @Schema(description = "생성·갱신 경로 코드. update_trigger_cd (`RECORD_INGESTION` / `MCP_USER_EDIT`), 정의되지 않은 코드는 그대로", example = "RECORD_INGESTION")
    String updateTrigger,

    @Schema(description = "생성·갱신 경로 라벨. 정의되지 않은 코드면 null", example = "파이프라인 Record 반영", nullable = true)
    String updateTriggerLabel,

    @Schema(description = "갱신 사유. update_reason, 현재 파이프라인은 null", nullable = true)
    String updateReason,

    @ArraySchema(schema = @Schema(description = "이번 갱신에서 파이프라인이 기록한 변경 본문 컬럼명. changed_fields_json (event_title, one_line_summary, key_bullets_json, detail_analysis 중). 두 버전 간 전체 차이가 아님", example = "event_title"))
    List<String> changedFields,

    @Schema(description = "당시 이벤트 제목. event_title")
    String eventName,

    @Schema(description = "당시 한줄 요약. one_line_summary")
    String oneLineSummary,

    @Schema(description = "Snapshot 당시 Event 최종 수정 일시 (KST). event_mod_dtm, 응답 시 KST 변환", example = "2026-09-21 11:28:50")
    LocalDateTime eventModifiedAt

) {

    public static EventSnapshotSummaryRes from(EventSnapshotListRow row) {
        return new EventSnapshotSummaryRes(
            row.miEventSnapshotId(),
            row.miEventId(),
            row.snapshotVersionNo(),
            row.previousMiEventSnapshotId(),
            DateUtil.utcToKst(row.createDt()),
            row.createUserId(),
            row.updateTriggerCd(),
            updateTriggerLabel(row.updateTriggerCd()),
            row.updateReason(),
            JsonUtil.parseStringList(row.changedFieldsJson()),
            row.eventTitle(),
            row.oneLineSummary(),
            DateUtil.utcToKst(row.eventModifyDt()));
    }

    static String updateTriggerLabel(String updateTriggerCd) {
        return EventSnapshotUpdateTrigger.fromCode(updateTriggerCd)
            .map(EventSnapshotUpdateTrigger::getLabel)
            .orElse(null);
    }

}

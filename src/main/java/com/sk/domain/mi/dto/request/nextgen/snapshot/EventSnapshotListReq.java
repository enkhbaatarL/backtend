package com.sk.domain.mi.dto.request.nextgen.snapshot;

import com.sk.domain.shared.enums.nextgen.EventSnapshotUpdateTrigger;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

/** [MCP Snapshot] 이벤트 Snapshot 버전 목록 요청 (날짜는 KST, page=1·size=20 기본). */
public record EventSnapshotListReq(

    @Schema(description = "Snapshot 저장일 시작 (KST, 포함). tmm_mi_mievent_snapshot_m.crt_dtm 기준", example = "2026-09-15")
    LocalDate startDate,

    @Schema(description = "Snapshot 저장일 종료 (KST, 포함). tmm_mi_mievent_snapshot_m.crt_dtm 기준", example = "2026-09-30")
    LocalDate endDate,

    @Schema(description = "생성·갱신 경로 (`RECORD_INGESTION`: 파이프라인 / `MCP_USER_EDIT`: MCP 사용자 수정). update_trigger_cd 일치",
        implementation = EventSnapshotUpdateTrigger.class, nullable = true)
    EventSnapshotUpdateTrigger updateTrigger,

    @Schema(description = "Snapshot 생성 주체 ID (대소문자 무시 일치). crt_usr_logn_id, 파이프라인은 `system`", example = "system")
    String createdBy,

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    Integer page,

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Schema(description = "페이지당 항목 수", example = "20", defaultValue = "20")
    Integer size

) {

    public EventSnapshotListReq {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }

}

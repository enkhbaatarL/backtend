package com.sk.domain.mi.dto.response.nextgen.snapshot;

import com.sk.domain.shared.enums.nextgen.EventSnapshotCompareTarget;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [MCP Snapshot] Snapshot 비교 결과. differences 는 두 상태를 직접 비교한 결과이고, recordedChangedFields 는 파이프라인이 기록한 값이다.
 */
public record EventSnapshotCompareRes(

    @Schema(description = "Event ID", example = "1269")
    Long eventId,

    @Schema(description = "비교 대상 (`previous` / `current`)", example = "previous")
    EventSnapshotCompareTarget target,

    @Schema(description = "선택한 Snapshot")
    Side base,

    @Schema(description = "비교 대상. previous 는 직전 Snapshot, current 는 현재 Event. 없으면 null", nullable = true)
    Side compared,

    @Schema(description = "비교 대상이 있는지. previous 인데 previousSnapshotId 가 없거나 current 인데 현재 Event 행이 없으면 false", example = "true")
    boolean comparable,

    @ArraySchema(schema = @Schema(description = "값이 다른 필드만"))
    List<FieldDifference> differences,

    @ArraySchema(schema = @Schema(description = "값이 같은 필드명", example = "eventTypeL2Id"))
    List<String> sameFields,

    @ArraySchema(schema = @Schema(description = "선택 Snapshot 의 changed_fields_json 그대로. 파이프라인이 기록한 본문 4개 컬럼의 직전·직후 비교 결과이며 differences 와 다를 수 있음", example = "one_line_summary"))
    List<String> recordedChangedFields

) {

    @Schema(name = "EventSnapshotCompareSide", description = "비교 한쪽의 식별 정보")
    public record Side(

        @Schema(description = "Snapshot ID. 현재 Event 이면 null", example = "12", nullable = true)
        Long snapshotId,

        @Schema(description = "버전 번호. 현재 Event 이면 null", example = "4", nullable = true)
        Long versionNo,

        @Schema(description = "Snapshot 저장 일시 (KST). 현재 Event 이면 null", nullable = true)
        LocalDateTime snapshotCreatedAt,

        @Schema(description = "Event 최종 수정 일시 (KST). Snapshot 이면 event_mod_dtm, 현재 Event 이면 tmm_mi_mievent_m.mod_dtm", nullable = true)
        LocalDateTime eventModifiedAt

    ) {
    }

    @Schema(name = "EventSnapshotFieldDifference", description = "값이 다른 필드")
    public record FieldDifference(

        @Schema(description = "응답 필드명 (Snapshot 상세 기준)", example = "oneLineSummary")
        String field,

        @Schema(description = "원본 컬럼명", example = "one_line_summary")
        String column,

        @Schema(description = "선택 Snapshot 의 값", nullable = true)
        Object baseValue,

        @Schema(description = "비교 대상의 값", nullable = true)
        Object comparedValue

    ) {
    }

}

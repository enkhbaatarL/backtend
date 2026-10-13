package com.sk.domain.mi.dto.response.nextgen.snapshot;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.shared.entity.master.tmm.MiEventSnapshot;
import com.sk.domain.shared.enums.nextgen.ContentStatus;
import com.sk.domain.shared.enums.nextgen.EventSignalType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [MCP Snapshot] Snapshot 버전 상세 = 목록 항목 필드 + 당시 Event 본문·속성 전체.
 */
public record EventSnapshotDetailRes(

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

    @Schema(description = "Snapshot 생성 주체 ID. crt_usr_logn_id", example = "system")
    String createdBy,

    @Schema(description = "생성·갱신 경로 코드. update_trigger_cd, 정의되지 않은 코드는 그대로", example = "RECORD_INGESTION")
    String updateTrigger,

    @Schema(description = "생성·갱신 경로 라벨. 정의되지 않은 코드면 null", example = "파이프라인 Record 반영", nullable = true)
    String updateTriggerLabel,

    @Schema(description = "갱신 사유. update_reason", nullable = true)
    String updateReason,

    @ArraySchema(schema = @Schema(description = "이번 갱신에서 파이프라인이 기록한 변경 본문 컬럼명. changed_fields_json. 두 버전 간 전체 차이가 아님", example = "one_line_summary"))
    List<String> changedFields,

    @Schema(description = "당시 이벤트명. one_line_summary")
    String eventName,

    @Schema(description = "당시 한줄 요약. one_line_summary")
    String oneLineSummary,

    @ArraySchema(schema = @Schema(description = "당시 핵심 요약. key_bullets_json"))
    List<String> keySummaries,

    @Schema(description = "당시 상세 분석. detail_analysis")
    String detailedAnalysis,

    @Schema(description = "당시 이벤트 유형 L2 ID. event_type_l2_id", example = "3")
    Long eventTypeL2Id,

    @ArraySchema(schema = @Schema(description = "당시 태그. tags_json", example = "LNG"))
    List<String> tags,

    @Schema(description = "이벤트 실제 발생 일시 (KST). occurred_at, 응답 시 KST 변환", nullable = true)
    LocalDateTime occurredAt,

    @Schema(description = "당시 Event 관련 Record 중 최신 first_created (KST). latest_record_first_created, 응답 시 KST 변환", nullable = true)
    LocalDateTime latestRecordFirstCreated,

    @Schema(description = "당시 Event 관련 Record 중 가장 오래된 first_created (KST). oldest_record_first_created, 응답 시 KST 변환", nullable = true)
    LocalDateTime oldestRecordFirstCreated,

    @Schema(description = "당시 Impact 연결 ID. estimated_impact_id", nullable = true)
    String estimatedImpactId,

    @Schema(description = "신호/소음 구분. signal_yn", example = "signal")
    EventSignalType signalType,

    @Schema(description = "신호/소음 라벨", example = "신호")
    String signalTypeLabel,

    @Schema(description = "신호 신뢰도 0~1. signal_confidence", example = "0.8500", nullable = true)
    BigDecimal signalConfidence,

    @Schema(description = "신호 판정 근거. signal_reason", nullable = true)
    String signalReason,

    @Schema(description = "시나리오화 추천 여부. scenario_suggest_yn", example = "false")
    Boolean isScenarioSuggested,

    @Schema(description = "시나리오화 추천 근거. scenario_suggest_reason", nullable = true)
    String scenarioSuggestReason,

    @Schema(description = "신규/갱신 상태. new_yn", example = "updated")
    ContentStatus status,

    @Schema(description = "신규/갱신 상태 라벨", example = "갱신")
    String statusLabel,

    @Schema(description = "Snapshot 저장 당시 원본 Event 의 삭제 여부. event_del_yn. 이력 자체의 삭제 여부가 아님", example = "false")
    Boolean isEventDeleted,

    @Schema(description = "당시 기관 전망 종합문. institution_view_synthesis", nullable = true)
    String institutionViewSynthesis,

    @Schema(description = "원본 Event 생성자 ID. event_crt_usr_logn_id", example = "system")
    String eventCreatedBy,

    @Schema(description = "원본 Event 생성 일시 (KST). event_crt_dtm, 응답 시 KST 변환")
    LocalDateTime eventCreatedAt,

    @Schema(description = "Snapshot 당시 Event 최종 수정자 ID. event_mod_usr_logn_id", example = "system")
    String eventModifiedBy,

    @Schema(description = "Snapshot 당시 Event 최종 수정 일시 (KST). event_mod_dtm, 응답 시 KST 변환")
    LocalDateTime eventModifiedAt

) {

    public static EventSnapshotDetailRes from(MiEventSnapshot snapshot) {
        EventSignalType signalType = Boolean.TRUE.equals(snapshot.getIsSignal()) ? EventSignalType.SIGNAL : EventSignalType.NOISE;
        ContentStatus status = Boolean.TRUE.equals(snapshot.getIsNew()) ? ContentStatus.NEW : ContentStatus.UPDATED;

        return new EventSnapshotDetailRes(
            snapshot.getMiEventSnapshotId(),
            snapshot.getMiEventId(),
            snapshot.getSnapshotVersionNo(),
            snapshot.getPreviousMiEventSnapshotId(),
            DateUtil.utcToKst(snapshot.getCreateDt()),
            snapshot.getCreateUserId(),
            snapshot.getUpdateTriggerCd(),
            EventSnapshotSummaryRes.updateTriggerLabel(snapshot.getUpdateTriggerCd()),
            snapshot.getUpdateReason(),
            JsonUtil.parseStringList(snapshot.getChangedFieldsJson()),
            snapshot.getOneLineSummary(),
            snapshot.getOneLineSummary(),
            JsonUtil.parseWrappedStringList(snapshot.getKeyBulletsJson(), "bullets"),
            snapshot.getDetailAnalysis(),
            snapshot.getEventTypeL2Id(),
            JsonUtil.parseWrappedStringList(snapshot.getTagsJson(), "tags"),
            DateUtil.utcToKst(snapshot.getOccurredAt()),
            DateUtil.utcToKst(snapshot.getLatestRecordFirstCreated()),
            DateUtil.utcToKst(snapshot.getOldestRecordFirstCreated()),
            snapshot.getEstimatedImpactId(),
            signalType,
            signalType.getLabel(),
            snapshot.getSignalConfidence(),
            snapshot.getSignalReason(),
            Boolean.TRUE.equals(snapshot.getIsScenarioSuggested()),
            snapshot.getScenarioSuggestReason(),
            status,
            status.getLabel(),
            Boolean.TRUE.equals(snapshot.getIsEventDeleted()),
            snapshot.getInstitutionViewSynthesis(),
            snapshot.getEventCreateUserId(),
            DateUtil.utcToKst(snapshot.getEventCreateDt()),
            snapshot.getEventModifyUserId(),
            DateUtil.utcToKst(snapshot.getEventModifyDt()));
    }

}

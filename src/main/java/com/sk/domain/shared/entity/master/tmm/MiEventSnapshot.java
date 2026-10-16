package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [MCP Snapshot] Event 버전 이력(Snapshot). 파이프라인이 Event 생성·갱신 직후 상태를 저장한다 (append-only, 조회 전용).
 * 저장 시각은 BaseEntity 의 crt_dtm, 행 논리 삭제는 del_yn 이며 당시 Event 삭제 여부(event_del_yn)와 다르다.
 */
@Getter
@Entity
@Immutable
@Comment("MI Event Enrichment 변경 이력 스냅샷")
@Table(name = "tmm_mi_mievent_snapshot_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEventSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mi_event_snapshot_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI Event Snapshot PK")
    private Long miEventSnapshotId;

    @Column(name = "mi_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("원본 MI Event ID")
    private Long miEventId;

    @Column(name = "snapshot_version_no", columnDefinition = "int unsigned")
    @Comment("Event 내부 버전 번호")
    private Long snapshotVersionNo;

    @Column(name = "previous_mi_event_snapshot_id", columnDefinition = "int unsigned")
    @Comment("직전 Snapshot ID")
    private Long previousMiEventSnapshotId;

    @Column(name = "update_trigger_cd", length = 30, nullable = false)
    @Comment("생성·갱신 경로 코드")
    private String updateTriggerCd;

    @Column(name = "update_reason", columnDefinition = "text")
    @Comment("갱신 사유")
    private String updateReason;

    @Column(name = "changed_fields_json", columnDefinition = "json")
    @Comment("이번 갱신에서 변경된 본문 컬럼명 목록 JSON")
    private String changedFieldsJson;

    @Column(name = "event_type_l2_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("당시 이벤트 유형 L2 ID")
    private Long eventTypeL2Id;

    @Column(name = "one_line_summary", length = 300, nullable = false)
    @Comment("한줄 요약")
    private String oneLineSummary;

    @Column(name = "key_bullets_json", columnDefinition = "json", nullable = false)
    @Comment("핵심 bullet 목록 JSON")
    private String keyBulletsJson;

    @Column(name = "detail_analysis", columnDefinition = "longtext", nullable = false)
    @Comment("이벤트 상세 분석 본문")
    private String detailAnalysis;

    @Column(name = "tags_json", columnDefinition = "json")
    @Comment("이벤트 태그 목록 JSON")
    private String tagsJson;

    @Column(name = "occurred_at")
    @Comment("이벤트 실제 발생 일시")
    private LocalDateTime occurredAt;

    @Column(name = "latest_record_first_created")
    @Comment("Event 관련 Record 중 최신 first_created 시각")
    private LocalDateTime latestRecordFirstCreated;

    @Column(name = "estimated_impact_id", length = 100)
    @Comment("Impact ID")
    private String estimatedImpactId;

    @Column(name = "signal_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("신호 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isSignal;

    @Column(name = "signal_confidence", precision = 5, scale = 4)
    @Comment("신호 신뢰도 0~1")
    private BigDecimal signalConfidence;

    @Column(name = "signal_reason", length = 100)
    @Comment("신호 판정 근거")
    private String signalReason;

    @Column(name = "scenario_suggest_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("시나리오화 추천 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isScenarioSuggested;

    @Column(name = "scenario_suggest_reason", length = 500)
    @Comment("시나리오화 추천 근거")
    private String scenarioSuggestReason;

    @Column(name = "new_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("신규생성 여부 (true: 신규생성, false: 갱신)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isNew;

    @Column(name = "event_del_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("Snapshot 저장 당시 원본 Event 삭제 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isEventDeleted;

    @Column(name = "oldest_record_first_created")
    @Comment("Event 관련 Record 중 오래된 first_created 시각")
    private LocalDateTime oldestRecordFirstCreated;

    @Column(name = "institution_view_synthesis", length = 1000)
    @Comment("기관 전망 View 종합")
    private String institutionViewSynthesis;

    @Column(name = "event_crt_usr_logn_id", length = 100, nullable = false)
    @Comment("원본 Event 생성자 ID")
    private String eventCreateUserId;

    @Column(name = "event_crt_dtm", nullable = false)
    @Comment("원본 Event 생성 일시")
    private LocalDateTime eventCreateDt;

    @Column(name = "event_mod_usr_logn_id", length = 100, nullable = false)
    @Comment("Snapshot 당시 Event 최종 수정자 ID")
    private String eventModifyUserId;

    @Column(name = "event_mod_dtm", nullable = false)
    @Comment("Snapshot 당시 Event 최종 수정 일시")
    private LocalDateTime eventModifyDt;

    /** 테스트 데이터용. 운영 코드에서 저장하지 않는다 (@Immutable). */
    @Builder
    public MiEventSnapshot(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
                           Long miEventSnapshotId, Long miEventId, Long snapshotVersionNo, Long previousMiEventSnapshotId,
                           String updateTriggerCd, String updateReason, String changedFieldsJson, Long eventTypeL2Id,
                           String oneLineSummary, String keyBulletsJson, String detailAnalysis, String tagsJson,
                           LocalDateTime occurredAt, LocalDateTime latestRecordFirstCreated, String estimatedImpactId,
                           Boolean isSignal, BigDecimal signalConfidence, String signalReason,
                           Boolean isScenarioSuggested, String scenarioSuggestReason, Boolean isNew, Boolean isEventDeleted,
                           LocalDateTime oldestRecordFirstCreated, String institutionViewSynthesis,
                           String eventCreateUserId, LocalDateTime eventCreateDt, String eventModifyUserId, LocalDateTime eventModifyDt) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.miEventSnapshotId = miEventSnapshotId;
        this.miEventId = miEventId;
        this.snapshotVersionNo = snapshotVersionNo;
        this.previousMiEventSnapshotId = previousMiEventSnapshotId;
        this.updateTriggerCd = updateTriggerCd;
        this.updateReason = updateReason;
        this.changedFieldsJson = changedFieldsJson;
        this.eventTypeL2Id = eventTypeL2Id;
        this.oneLineSummary = oneLineSummary;
        this.keyBulletsJson = keyBulletsJson;
        this.detailAnalysis = detailAnalysis;
        this.tagsJson = tagsJson;
        this.occurredAt = occurredAt;
        this.latestRecordFirstCreated = latestRecordFirstCreated;
        this.estimatedImpactId = estimatedImpactId;
        this.isSignal = isSignal;
        this.signalConfidence = signalConfidence;
        this.signalReason = signalReason;
        this.isScenarioSuggested = isScenarioSuggested;
        this.scenarioSuggestReason = scenarioSuggestReason;
        this.isNew = isNew;
        this.isEventDeleted = isEventDeleted;
        this.oldestRecordFirstCreated = oldestRecordFirstCreated;
        this.institutionViewSynthesis = institutionViewSynthesis;
        this.eventCreateUserId = eventCreateUserId;
        this.eventCreateDt = eventCreateDt;
        this.eventModifyUserId = eventModifyUserId;
        this.eventModifyDt = eventModifyDt;
    }

}

package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("MI Event 마스터")
@Table(name = "tmm_mi_mievent_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mi_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("MI Event PK")
    private Long miEventId;

    @Column(name = "event_title", length = 500, nullable = false)
    @Comment("이벤트 제목")
    private String eventTitle;

    @Column(name = "one_line_summary", length = 300)
    @Comment("한줄 요약")
    private String oneLineSummary;

    @Column(name = "key_bullets_json", columnDefinition = "json")
    @Comment("핵심 bullet 목록 JSON")
    private String keyBulletsJson;

    @Column(name = "event_summary", columnDefinition = "longtext", nullable = false)
    @Comment("이벤트 상세 분석 본문")
    private String eventSummary;

    @Column(name = "market_and_institution_view", columnDefinition = "longtext")
    @Comment("시장 반응 및 기관 전망")
    private String marketAndInstitutionView;

    @Column(name = "tags_json", columnDefinition = "json")
    @Comment("이벤트 태그 목록 JSON")
    private String tagsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_l2_id", nullable = false)
    @Comment("이벤트 유형 L2 FK")
    private MiEventTypeL2 eventTypeL2;

    @Column(name = "mi_event_status_code", length = 50)
    @Comment("이벤트 상태 코드 (rumored, expected, announced, in_progress, occurred, confirmed, resolved, cancelled)")
    private String miEventStatusCode;

    @Column(name = "estimated_impact_id", length = 100)
    @Comment("Impact ID")
    private String estimatedImpactId;

    @Column(name = "occurred_at")
    @Comment("이벤트 실제 발생 일시")
    private LocalDateTime occurredAt;

    @Column(name = "probability", precision = 5, scale = 4, nullable = false)
    @Comment("이벤트 발생 확률 0.0000 ~ 1.0000")
    private BigDecimal probability;

    @Column(name = "dedup_state", length = 20, nullable = false)
    @Comment("중복처리 상태 (canonical, duplicate, merged)")
    private String dedupState;

    @Column(name = "signal_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("신호 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean signalYn;

    @Column(name = "signal_confidence", precision = 5, scale = 4, nullable = false)
    @Comment("신호 신뢰도 0~1")
    private BigDecimal signalConfidence;

    @Column(name = "signal_reason", length = 500)
    @Comment("신호 판정 근거")
    private String signalReason;

    @Builder
    public MiEvent(Long miEventId, String eventTitle, String oneLineSummary, String keyBulletsJson,
            String eventSummary, String marketAndInstitutionView, String tagsJson,
            MiEventTypeL2 eventTypeL2, String miEventStatusCode, String estimatedImpactId,
            LocalDateTime occurredAt, BigDecimal probability, String dedupState,
            Boolean signalYn, BigDecimal signalConfidence, String signalReason) {
        this.miEventId = miEventId;
        this.eventTitle = eventTitle;
        this.oneLineSummary = oneLineSummary;
        this.keyBulletsJson = keyBulletsJson;
        this.eventSummary = eventSummary;
        this.marketAndInstitutionView = marketAndInstitutionView;
        this.tagsJson = tagsJson;
        this.eventTypeL2 = eventTypeL2;
        this.miEventStatusCode = miEventStatusCode;
        this.estimatedImpactId = estimatedImpactId;
        this.occurredAt = occurredAt;
        this.probability = probability;
        this.dedupState = dedupState;
        this.signalYn = signalYn;
        this.signalConfidence = signalConfidence;
        this.signalReason = signalReason;
    }
}

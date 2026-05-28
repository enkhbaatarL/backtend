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

    @Column(name = "one_line_summary", length = 300, nullable = false)
    @Comment("한줄 요약")
    private String oneLineSummary;

    @Column(name = "key_bullets_json", columnDefinition = "json", nullable = false)
    @Comment("핵심 bullet 목록 JSON")
    private String keyBulletsJson;

    @Column(name = "detail_analysis", columnDefinition = "longtext", nullable = false)
    @Comment("이벤트 상세 분석 본문")
    private String detailAnalysis;

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

    @Column(name = "estimated_impact_id", length = 100)
    @Comment("Impact ID")
    private String estimatedImpactId;

    @Column(name = "occurred_at")
    @Comment("이벤트 실제 발생 일시")
    private LocalDateTime occurredAt;

    @Column(name = "signal_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("신호 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isSignal;

    @Column(name = "signal_confidence", precision = 5, scale = 4)
    @Comment("신호 신뢰도 0~1")
    private java.math.BigDecimal signalConfidence;

    @Column(name = "signal_reason", length = 100)
    @Comment("신호 판정 근거")
    private String signalReason;

    @Column(name = "scenario_suggest_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("시나리오화 추천 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isScenarioSuggested;

    @Column(name = "scenario_suggest_reason", length = 100)
    @Comment("시나리오화 추천 근거")
    private String scenarioSuggestReason;

    @Column(name = "new_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("신규생성 여부 (true: 신규생성, false: 갱신)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isNew;

    @Builder
    public MiEvent(Long miEventId, String eventTitle, String oneLineSummary, String keyBulletsJson,
                   String detailAnalysis, String marketAndInstitutionView, String tagsJson,
                   MiEventTypeL2 eventTypeL2, String estimatedImpactId,
                   LocalDateTime occurredAt,
                   Boolean isSignal, BigDecimal signalConfidence, String signalReason,
                   Boolean isScenarioSuggested, String scenarioSuggestReason, Boolean isNew) {
        this.miEventId = miEventId;
        this.eventTitle = eventTitle;
        this.oneLineSummary = oneLineSummary;
        this.keyBulletsJson = keyBulletsJson;
        this.detailAnalysis = detailAnalysis;
        this.marketAndInstitutionView = marketAndInstitutionView;
        this.tagsJson = tagsJson;
        this.eventTypeL2 = eventTypeL2;
        this.estimatedImpactId = estimatedImpactId;
        this.occurredAt = occurredAt;
        this.isSignal = isSignal;
        this.signalConfidence = signalConfidence;
        this.signalReason = signalReason;
        this.isScenarioSuggested = isScenarioSuggested;
        this.scenarioSuggestReason = scenarioSuggestReason;
        this.isNew = isNew;
    }

}

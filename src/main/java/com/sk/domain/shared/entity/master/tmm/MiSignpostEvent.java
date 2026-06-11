package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
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
@Comment("Signpost 이벤트")
@Table(name = "tmm_mi_signpost_event_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiSignpostEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signpost_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("Signpost 이벤트 ID")
    private Long signpostEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signpost_id", nullable = false)
    @Comment("Signpost ID")
    private MiSignpost signpost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mi_event_id")
    @Comment("연결 MI 이벤트 ID")
    private MiEvent miEvent;

    @Column(name = "occurred_at")
    @Comment("Signpost 이벤트 발생일시")
    private LocalDateTime occurredAt;

    @Column(name = "summary", columnDefinition = "text", nullable = false)
    @Comment("signpost 이벤트 요약")
    private String summary;

    @Column(name = "probability_score", precision = 5, scale = 2)
    @Comment("시나리오 확률 업데이트 확률")
    private BigDecimal probabilityScore;

    @Column(name = "probability_delta", precision = 5, scale = 2)
    @Comment("시나리오 확률 업데이트 조정")
    private BigDecimal probabilityDelta;

    @Column(name = "matching_reason", columnDefinition = "text")
    @Comment("상세내역 (signpost 매칭 근거)")
    private String matchingReason;

    @Builder
    public MiSignpostEvent(Long signpostEventId, MiSignpost signpost, MiEvent miEvent, LocalDateTime occurredAt,
                           String summary, BigDecimal probabilityScore, BigDecimal probabilityDelta,
                           String matchingReason) {
        this.signpostEventId = signpostEventId;
        this.signpost = signpost;
        this.miEvent = miEvent;
        this.occurredAt = occurredAt;
        this.summary = summary;
        this.probabilityScore = probabilityScore;
        this.probabilityDelta = probabilityDelta;
        this.matchingReason = matchingReason;
    }

}

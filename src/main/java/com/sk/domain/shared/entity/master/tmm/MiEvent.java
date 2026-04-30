package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// note: DB PK는 (mi_event_id, first_created) 복합키지만 mi_event_id 단독으로 unique 하므로 JPA는 단일 PK로 매핑
@Getter
@Entity
@Comment("TMM_MI이벤트")
@Table(name = "tmm_mieventm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiEvent extends BaseEntity {

    @Id
    @Column(name = "mi_event_id", length = 32, nullable = false)
    @Comment("MI Event ID")
    private String miEventId;

    @Column(name = "first_created", nullable = false)
    @Comment("Event 최초생성일시")
    private LocalDateTime firstCreated;

    @Column(name = "event_title_ko", length = 500, nullable = false)
    @Comment("이벤트 제목 (한국어)")
    private String eventTitleKo;

    @Column(name = "one_line_summary", length = 200, nullable = false)
    @Comment("1줄 요약 [토픽] 에피소드 → 영향")
    private String oneLineSummary;

    @Column(name = "detail_analysis", columnDefinition = "text")
    @Comment("상세 분석 내용")
    private String detailAnalysis;

    @Column(name = "market_and_institution_view", columnDefinition = "text")
    @Comment("시장 반응 및 기관 전망")
    private String marketAndInstitutionView;

    @Column(name = "language", length = 10)
    @Comment("문서 언어")
    private String language;

    @Column(name = "occurred_at")
    @Comment("이벤트 실제 발생일시")
    private LocalDateTime occurredAt;

    @Column(name = "probability", precision = 3, scale = 2)
    @Comment("이벤트 발생 확률 0.00 ~ 1.00")
    private BigDecimal probability;

    @Column(name = "dedup_state", length = 30)
    @Comment("중복처리 상태 (canonical, duplicate, merged)")
    private String dedupState;

    @Column(name = "level_1_code", length = 100)
    @Comment("이벤트 유형 레벨1 코드")
    private String level1Code;

    @Column(name = "level_2_code", length = 100)
    @Comment("이벤트 유형 레벨2 코드")
    private String level2Code;

    @Column(name = "taxonomy_version", length = 30)
    @Comment("분류체계 버전")
    private String taxonomyVersion;

    @Column(name = "status_code", length = 30)
    @Comment("이벤트 상태 코드")
    private String statusCode;

    @Column(name = "status_label_ko", length = 100)
    @Comment("이벤트 상태 라벨 (한국어)")
    private String statusLabelKo;

    @Column(name = "status_taxonomy_version", length = 30)
    @Comment("상태 분류체계 버전")
    private String statusTaxonomyVersion;

    @Builder
    public MiEvent(String miEventId, LocalDateTime firstCreated, String eventTitleKo, String oneLineSummary,
            String detailAnalysis, String marketAndInstitutionView, String language,
            LocalDateTime occurredAt, BigDecimal probability, String dedupState,
            String level1Code, String level2Code, String taxonomyVersion,
            String statusCode, String statusLabelKo, String statusTaxonomyVersion) {
        this.miEventId = miEventId;
        this.firstCreated = firstCreated;
        this.eventTitleKo = eventTitleKo;
        this.oneLineSummary = oneLineSummary;
        this.detailAnalysis = detailAnalysis;
        this.marketAndInstitutionView = marketAndInstitutionView;
        this.language = language;
        this.occurredAt = occurredAt;
        this.probability = probability;
        this.dedupState = dedupState;
        this.level1Code = level1Code;
        this.level2Code = level2Code;
        this.taxonomyVersion = taxonomyVersion;
        this.statusCode = statusCode;
        this.statusLabelKo = statusLabelKo;
        this.statusTaxonomyVersion = statusTaxonomyVersion;
    }
}

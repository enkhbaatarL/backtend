package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMM_QualitativeImpact")
@Table(name = "tmm_qualimpactm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverrides({
        @AttributeOverride(name = "createUserId", column = @Column(name = "crt_user_id", length = 50, nullable = false)),
        @AttributeOverride(name = "modifyUserId", column = @Column(name = "mod_user_id", length = 50, nullable = false))
})
public class QualitativeImpact extends BaseEntity {

    @Id
    @Column(name = "impact_result_id", length = 64, nullable = false)
    @Comment("Impact Result ID FK (PK)")
    private String impactResultId;

    @Column(name = "driver_tree_id", length = 64, nullable = false)
    @Comment("Driver Tree ID")
    private String driverTreeId;

    @Column(name = "completed", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("분석 완료 여부 Y/N")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean completed;

    @Column(name = "analysis_time")
    @Comment("분석 완료 일시")
    private LocalDateTime analysisTime;

    @Column(name = "direction", length = 20)
    @Comment("방향 bullish|bearish|negligible")
    private String direction;

    @Column(name = "magnitude_level", length = 20)
    @Comment("임팩트 레벨 low|medium|high|extreme")
    private String magnitudeLevel;

    @Column(name = "driver_magnitude", length = 20)
    @Comment("드라이버 영향도")
    private String driverMagnitude;

    @Column(name = "matched_keywords", columnDefinition = "json")
    @Comment("매칭 키워드 목록 (JSON)")
    private String matchedKeywords;

    @Column(name = "impact_assessment_reason", columnDefinition = "text")
    @Comment("임팩트 평가 사유")
    private String impactAssessmentReason;

    @Column(name = "confidence", precision = 5, scale = 4)
    @Comment("신뢰도 0~1")
    private BigDecimal confidence;

    @Builder
    public QualitativeImpact(String impactResultId, String driverTreeId, Boolean completed,
            LocalDateTime analysisTime, String direction, String magnitudeLevel,
            String driverMagnitude, String matchedKeywords, String impactAssessmentReason,
            BigDecimal confidence) {
        this.impactResultId = impactResultId;
        this.driverTreeId = driverTreeId;
        this.completed = completed;
        this.analysisTime = analysisTime;
        this.direction = direction;
        this.magnitudeLevel = magnitudeLevel;
        this.driverMagnitude = driverMagnitude;
        this.matchedKeywords = matchedKeywords;
        this.impactAssessmentReason = impactAssessmentReason;
        this.confidence = confidence;
    }
}

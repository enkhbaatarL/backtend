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
@Comment("TMM_ImpactResult")
@Table(name = "tmm_impactresultm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverrides({
        @AttributeOverride(name = "createUserId", column = @Column(name = "crt_user_id", length = 50, nullable = false)),
        @AttributeOverride(name = "modifyUserId", column = @Column(name = "mod_user_id", length = 50, nullable = false))
})
public class ImpactResult extends BaseEntity {

    @Id
    @Column(name = "impact_result_id", length = 64, nullable = false)
    @Comment("Impact Result ID")
    private String impactResultId;

    @Column(name = "impact_id", length = 64, nullable = false)
    @Comment("Impact ID FK")
    private String impactId;

    @Column(name = "target_type", length = 20, nullable = false)
    @Comment("대상 유형 price|spread")
    private String targetType;

    @Column(name = "target_id", length = 20, nullable = false)
    @Comment("대상 ID JKM|TTF|HH|JKM-HH")
    private String targetId;

    @Column(name = "direction", length = 20)
    @Comment("방향 bullish|bearish|negligible")
    private String direction;

    @Column(name = "confidence", precision = 5, scale = 4)
    @Comment("신뢰도 0~1")
    private BigDecimal confidence;

    @Column(name = "impact_assessment_reason", columnDefinition = "text")
    @Comment("임팩트 평가 사유")
    private String impactAssessmentReason;

    @Column(name = "completed", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("분석 완료 여부 Y/N")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean completed;

    @Column(name = "analysis_time")
    @Comment("분석 완료 일시")
    private LocalDateTime analysisTime;

    @Builder
    public ImpactResult(String impactResultId, String impactId, String targetType, String targetId,
            String direction, BigDecimal confidence, String impactAssessmentReason,
            Boolean completed, LocalDateTime analysisTime) {
        this.impactResultId = impactResultId;
        this.impactId = impactId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.direction = direction;
        this.confidence = confidence;
        this.impactAssessmentReason = impactAssessmentReason;
        this.completed = completed;
        this.analysisTime = analysisTime;
    }
}

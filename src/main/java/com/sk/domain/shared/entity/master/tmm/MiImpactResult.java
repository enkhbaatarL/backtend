package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.scenarioworkbench.ImpactGrade;
import com.sk.domain.shared.converter.ImpactGradeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Comment("Impact Result 마스터")
@Table(name = "tmm_mi_impact_result_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiImpactResult extends BaseEntity {

    @Id
    @Column(name = "impact_result_id", length = 64, nullable = false)
    @Comment("Impact Result ID PK")
    private String impactResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impact_id", nullable = false)
    @Comment("Impact ID FK")
    private MiImpact impact;

    @Column(name = "target_type", length = 20, nullable = false)
    @Comment("대상 유형 price|spread")
    private String targetType;

    @Column(name = "target_id", length = 20, nullable = false)
    @Comment("대상 ID JKM/TTF/HH")
    private String targetId;

    @Convert(converter = ImpactGradeConverter.class)
    @Column(name = "impact_short", nullable = false, columnDefinition = "enum('상승_상','상승_중','상승_하','영향없음','하락_하','하락_중','하락_상')")
    @Comment("단기 영향 (0~1M) {방향_강도}")
    private ImpactGrade impactShort;

    @Convert(converter = ImpactGradeConverter.class)
    @Column(name = "impact_mid", columnDefinition = "enum('상승_상','상승_중','상승_하','영향없음','하락_하','하락_중','하락_상')")
    @Comment("중기 영향 (6M). Base Case 없으면 NULL")
    private ImpactGrade impactMid;

    @Convert(converter = ImpactGradeConverter.class)
    @Column(name = "impact_long", columnDefinition = "enum('상승_상','상승_중','상승_하','영향없음','하락_하','하락_중','하락_상')")
    @Comment("장기 영향 (1Y+). Base Case 없으면 NULL")
    private ImpactGrade impactLong;

    @Column(name = "confidence", precision = 5, scale = 4)
    @Comment("신뢰도 0~1")
    private BigDecimal confidence;

    @Column(name = "rationale_base_case", columnDefinition = "text")
    @Comment("사용한 Base Case 가정")
    private String rationaleBaseCase;

    @Column(name = "rationale_driver_map", columnDefinition = "text")
    @Comment("Step 1 driver 영향 요약")
    private String rationaleDriverMap;

    @Column(name = "rationale_synthesis", columnDefinition = "text")
    @Comment("종합 판단 logic")
    private String rationaleSynthesis;

    @Column(name = "completed", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("분석 완료 여부 Y/N")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean completed;

    @Column(name = "analysis_time")
    @Comment("분석 완료 일시")
    private LocalDateTime analysisTime;

    @Builder
    public MiImpactResult(String impactResultId, MiImpact impact, String targetType, String targetId,
                          ImpactGrade impactShort, ImpactGrade impactMid, ImpactGrade impactLong, BigDecimal confidence,
                          String rationaleBaseCase, String rationaleDriverMap, String rationaleSynthesis,
                          Boolean completed, LocalDateTime analysisTime) {
        this.impactResultId = impactResultId;
        this.impact = impact;
        this.targetType = targetType;
        this.targetId = targetId;
        this.impactShort = impactShort;
        this.impactMid = impactMid;
        this.impactLong = impactLong;
        this.confidence = confidence;
        this.rationaleBaseCase = rationaleBaseCase;
        this.rationaleDriverMap = rationaleDriverMap;
        this.rationaleSynthesis = rationaleSynthesis;
        this.completed = completed;
        this.analysisTime = analysisTime;
    }

}

package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;

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
@Comment("시나리오")
@Table(name = "tmm_mi_scenario_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiScenario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오 ID")
    private Long scenarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_group_id", nullable = false)
    @Comment("시나리오 그룹 ID")
    private MiScenarioGroup scenarioGroup;

    @Column(name = "title", length = 255, nullable = false)
    @Comment("시나리오명")
    private String title;

    @Column(name = "narrative", columnDefinition = "text")
    @Comment("시나리오 내러티브")
    private String narrative;

    @Column(name = "dimension_values", columnDefinition = "json", nullable = false)
    @Comment("시나리오 주요 가정 (dimension values)")
    private String dimensionValues;

    @Column(name = "probability_score", precision = 5, scale = 2)
    @Comment("시나리오 발생확률")
    private BigDecimal probabilityScore;

    @Column(name = "probability_level", length = 50)
    @Comment("시나리오 확률 레벨")
    private String probabilityLevel;

    @Column(name = "probability_delta", precision = 5, scale = 2)
    @Comment("시나리오 확률 증감")
    private BigDecimal probabilityDelta;

    @Column(name = "impact_jkm", length = 50)
    @Comment("이벤트 임팩트(JKM)")
    private String impactJkm;

    @Column(name = "impact_ttf", length = 50)
    @Comment("이벤트 임팩트(TTF)")
    private String impactTtf;

    @Column(name = "impact_hh", length = 50)
    @Comment("이벤트 임팩트(HH)")
    private String impactHh;

    @Column(name = "feasibility_score")
    @Comment("실현가능성 점수")
    private Short feasibilityScore;

    @Column(name = "judgment_status_code", length = 50)
    @ColumnDefault("'대기'")
    @Comment("판정 상태 코드")
    private String judgmentStatusCode;

    @Column(name = "display_order", nullable = false)
    @ColumnDefault("0")
    @Comment("표시 순서")
    private Integer displayOrder;

    @Column(name = "baseline_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @ColumnDefault("'N'")
    @Comment("기준 시나리오 여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isBaseline;

    @Builder
    public MiScenario(Long scenarioId, MiScenarioGroup scenarioGroup, String title, String narrative,
                      String dimensionValues, BigDecimal probabilityScore, String probabilityLevel,
                      BigDecimal probabilityDelta, String impactJkm, String impactTtf, String impactHh,
                      Short feasibilityScore, String judgmentStatusCode, Integer displayOrder, Boolean isBaseline) {
        this.scenarioId = scenarioId;
        this.scenarioGroup = scenarioGroup;
        this.title = title;
        this.narrative = narrative;
        this.dimensionValues = dimensionValues;
        this.probabilityScore = probabilityScore;
        this.probabilityLevel = probabilityLevel;
        this.probabilityDelta = probabilityDelta;
        this.impactJkm = impactJkm;
        this.impactTtf = impactTtf;
        this.impactHh = impactHh;
        this.feasibilityScore = feasibilityScore;
        this.judgmentStatusCode = judgmentStatusCode;
        this.displayOrder = displayOrder;
        this.isBaseline = isBaseline;
    }

    /** 판정 상태 변경. DB 컬럼은 한글 라벨(대기/실현/폐기)로 적재한다. */
    public void updateJudgmentStatus(ScenarioStatus status) {
        this.judgmentStatusCode = status.getLabel();
    }

}

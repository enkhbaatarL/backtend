package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.domain.shared.converter.ProbabilityDirectionConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;

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
@Comment("Signpost")
@Table(name = "tmm_mi_signpost_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiSignpost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signpost_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("Signpost ID")
    private Long signpostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    @Comment("시나리오 ID")
    private MiScenario scenario;

    @Column(name = "label", length = 500, nullable = false)
    @Comment("signpost label")
    private String label;

    @Convert(converter = ProbabilityDirectionConverter.class)
    @Column(name = "probability_direction_code", length = 50)
    @Comment("확률 변화 방향")
    private ProbabilityDirection probabilityDirection;

    @Column(name = "probability_impact_score", precision = 5, scale = 2)
    @Comment("확률 변화 강도")
    private BigDecimal probabilityImpactScore;

    @Column(name = "signpost_reason", columnDefinition = "text")
    @Comment("signpost 근거")
    private String signpostReason;

    @Column(name = "display_order", nullable = false)
    @ColumnDefault("0")
    @Comment("표시 순서")
    private Integer displayOrder;

    @Builder
    public MiSignpost(Long signpostId, MiScenario scenario, String label, ProbabilityDirection probabilityDirection,
                      BigDecimal probabilityImpactScore, String signpostReason, Integer displayOrder) {
        this.signpostId = signpostId;
        this.scenario = scenario;
        this.label = label;
        this.probabilityDirection = probabilityDirection;
        this.probabilityImpactScore = probabilityImpactScore;
        this.signpostReason = signpostReason;
        this.displayOrder = displayOrder;
    }

}

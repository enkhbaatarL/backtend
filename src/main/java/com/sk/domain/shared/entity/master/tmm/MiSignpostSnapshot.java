package com.sk.domain.shared.entity.master.tmm;

import java.math.BigDecimal;

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
@Comment("Signpost 스냅샷")
@Table(name = "tmm_mi_signpost_snap_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiSignpostSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signpost_snap_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("Signpost 스냅샷 ID")
    private Long signpostSnapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    @Comment("스냅샷 ID")
    private MiSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_snap_id", nullable = false)
    @Comment("시나리오 스냅샷 ID (스냅샷 내부 부모)")
    private MiScenarioSnapshot scenarioSnapshot;

    @Column(name = "src_signpost_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("원본 Signpost ID (추적용, FK 미설정)")
    private Long srcSignpostId;

    @Column(name = "label", length = 500, nullable = false)
    @Comment("signpost label (동결)")
    private String label;

    @Convert(converter = ProbabilityDirectionConverter.class)
    @Column(name = "probability_direction_code", length = 50)
    @Comment("확률 변화 방향 (동결)")
    private ProbabilityDirection probabilityDirection;

    @Column(name = "probability_impact_score", precision = 5, scale = 2)
    @Comment("확률 변화 강도 (동결)")
    private BigDecimal probabilityImpactScore;

    @Builder
    public MiSignpostSnapshot(Long signpostSnapId, MiSnapshot snapshot, MiScenarioSnapshot scenarioSnapshot,
                              Long srcSignpostId, String label, ProbabilityDirection probabilityDirection,
                              BigDecimal probabilityImpactScore) {
        this.signpostSnapId = signpostSnapId;
        this.snapshot = snapshot;
        this.scenarioSnapshot = scenarioSnapshot;
        this.srcSignpostId = srcSignpostId;
        this.label = label;
        this.probabilityDirection = probabilityDirection;
        this.probabilityImpactScore = probabilityImpactScore;
    }

}

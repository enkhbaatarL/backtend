package com.sk.domain.shared.entity.master.tmm;

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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("시나리오 스냅샷")
@Table(name = "tmm_mi_scenario_snap_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiScenarioSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_snap_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오 스냅샷 ID")
    private Long scenarioSnapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    @Comment("스냅샷 ID")
    private MiSnapshot snapshot;

    @Column(name = "src_scenario_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("원본 시나리오 ID (추적용, FK 미설정)")
    private Long srcScenarioId;

    @Column(name = "dimension_values", columnDefinition = "json", nullable = false)
    @Comment("dimension 값 (스냅샷 시점 동결)")
    private String dimensionValues;

    @Column(name = "probability_score", precision = 5, scale = 2)
    @Comment("발생확률 (동결)")
    private BigDecimal probabilityScore;

    @Column(name = "baseline_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @ColumnDefault("'N'")
    @Comment("기준 시나리오 여부 (동결)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isBaseline;

    @Builder
    public MiScenarioSnapshot(Long scenarioSnapId, MiSnapshot snapshot, Long srcScenarioId, String dimensionValues,
                              BigDecimal probabilityScore, Boolean isBaseline) {
        this.scenarioSnapId = scenarioSnapId;
        this.snapshot = snapshot;
        this.srcScenarioId = srcScenarioId;
        this.dimensionValues = dimensionValues;
        this.probabilityScore = probabilityScore;
        this.isBaseline = isBaseline;
    }

}

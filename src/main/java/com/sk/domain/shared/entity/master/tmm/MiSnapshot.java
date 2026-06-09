package com.sk.domain.shared.entity.master.tmm;

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
@Comment("스냅샷")
@Table(name = "tmm_mi_snapshot_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("스냅샷 ID")
    private Long snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_group_id", nullable = false)
    @Comment("시나리오 그룹 ID")
    private MiScenarioGroup scenarioGroup;

    @Column(name = "stage_no", nullable = false)
    @Comment("단계 번호 (1·2)")
    private Short stageNo;

    @Column(name = "phase_code", length = 50, nullable = false)
    @Comment("단계 구분 (BEFORE/AFTER)")
    private String phaseCode;

    @Builder
    public MiSnapshot(Long snapshotId, MiScenarioGroup scenarioGroup, Short stageNo, String phaseCode) {
        this.snapshotId = snapshotId;
        this.scenarioGroup = scenarioGroup;
        this.stageNo = stageNo;
        this.phaseCode = phaseCode;
    }

}

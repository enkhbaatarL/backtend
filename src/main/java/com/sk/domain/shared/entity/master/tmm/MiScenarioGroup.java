package com.sk.domain.shared.entity.master.tmm;

import org.hibernate.annotations.ColumnDefault;
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
@Comment("시나리오 그룹")
@Table(name = "tmm_mi_scenario_group_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiScenarioGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_group_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오 그룹 ID")
    private Long scenarioGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mi_event_id", nullable = false, unique = true)
    @Comment("MI 이벤트 ID (1:1 기준 키)")
    private MiEvent miEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_l2_id", nullable = false)
    @Comment("이벤트 유형 L2 ID (분류 기준)")
    private MiEventTypeL2 eventTypeL2;

    @Column(name = "title", length = 255, nullable = false)
    @Comment("시나리오 그룹명")
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    @Comment("시나리오 그룹 요약")
    private String summary;

    @Column(name = "description", columnDefinition = "text")
    @Comment("시나리오 그룹 설명")
    private String description;

    @Column(name = "dimension_selected", columnDefinition = "json", nullable = false)
    @Comment("표시 컬럼 선택 (dim_key 배열)")
    private String dimensionSelected;

    @Column(name = "scenario_count", nullable = false)
    @ColumnDefault("0")
    @Comment("시나리오 수")
    private Integer scenarioCount;

    @Builder
    public MiScenarioGroup(Long scenarioGroupId, MiEvent miEvent, MiEventTypeL2 eventTypeL2, String title,
                           String summary, String description, String dimensionSelected, Integer scenarioCount) {
        this.scenarioGroupId = scenarioGroupId;
        this.miEvent = miEvent;
        this.eventTypeL2 = eventTypeL2;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.dimensionSelected = dimensionSelected;
        this.scenarioCount = scenarioCount;
    }

}

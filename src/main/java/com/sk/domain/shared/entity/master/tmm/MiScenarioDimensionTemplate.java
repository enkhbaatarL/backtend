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
@Comment("시나리오 Dimension Template")
@Table(name = "tmm_mi_scenario_dimension_template_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiScenarioDimensionTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_dimension_template_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오 Dimension Template ID")
    private Long scenarioDimensionTemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_l2_id", nullable = false, unique = true)
    @Comment("이벤트 유형 L2 ID")
    private MiEventTypeL2 eventTypeL2;

    @Column(name = "template_json", columnDefinition = "json", nullable = false)
    @Comment("Dimension 정의 배열 JSON")
    private String templateJson;

    @Builder
    public MiScenarioDimensionTemplate(Long scenarioDimensionTemplateId, MiEventTypeL2 eventTypeL2, String templateJson) {
        this.scenarioDimensionTemplateId = scenarioDimensionTemplateId;
        this.eventTypeL2 = eventTypeL2;
        this.templateJson = templateJson;
    }

}

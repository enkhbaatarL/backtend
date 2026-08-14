package com.sk.domain.shared.entity.master.tmm;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 시나리오 그룹 - 이벤트 매핑 (N:M).
 *
 * 멀티 이벤트 전환(migration 0022/0023)으로 그룹의 직접 이벤트 컬럼이 제거되고 이 테이블이 관계의 원본이 되었다.
 * 적재 주체는 Agent이며 B.E는 조회 전용이다. UNIQUE (scenario_group_id, mi_event_id).
 *
 * eventTypeL2는 그룹 생성 시점에 동결된 값이다. 이벤트의 L2가 사후 변경되어도 이 값은 갱신되지 않으므로,
 * 판정 로직(dimension template 조회, ModelInputMode)은 이 동결값을 신뢰한다.
 *
 * scenarioDimensionTemplateId와 templateChecksum은 v1-v3 template revision 고정용 슬롯으로,
 * 0023 backfill 데이터와 v4 그룹에서는 null이다. B.E는 현재 읽지 않는다.
 */
@Getter
@Entity
@Comment("시나리오 그룹 이벤트 매핑")
@Table(name = "tmm_mi_scenario_group_event_m")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiScenarioGroupEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_group_event_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("시나리오 그룹 이벤트 연결 ID")
    private Long scenarioGroupEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_group_id", nullable = false)
    @Comment("시나리오 그룹 ID")
    private MiScenarioGroup scenarioGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mi_event_id", nullable = false)
    @Comment("공동 전제 MI 이벤트 ID")
    private MiEvent miEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_l2_id", nullable = false)
    @Comment("이벤트 유형 L2 ID (그룹 생성 시점 동결)")
    private MiEventTypeL2 eventTypeL2;

    @Column(name = "scenario_dimension_template_id", columnDefinition = "int unsigned")
    @Comment("v1-v3 Dimension Template ID (revision 고정용)")
    private Long scenarioDimensionTemplateId;

    @Column(name = "template_checksum", length = 64)
    @Comment("v1-v3 Template checksum (revision 고정용)")
    private String templateChecksum;

    @Column(name = "display_order", nullable = false)
    @Comment("이벤트 표시 순서")
    private Integer displayOrder;

}

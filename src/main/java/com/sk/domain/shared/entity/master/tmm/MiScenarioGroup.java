package com.sk.domain.shared.entity.master.tmm;

import com.sk.domain.shared.converter.ScenarioGroupStageConverter;
import com.sk.domain.shared.converter.ScenarioGroupStatusConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 시나리오 그룹.
 *
 * 멀티 이벤트 전환(migration 0023)으로 직접 이벤트 컬럼(mi_event_id, event_type_l2_id)이 제거되었다.
 * 이벤트 연결의 원본은 매핑 테이블이며, 기준 이벤트가 필요한 조회는 매핑의 display_order 첫 행을 사용한다.
 * MiScenarioGroupEvent 참조.
 */
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
    @Comment("표시 컬럼 선택 (v1은 dim_key 배열, v4는 dimensionId 배열)")
    private String dimensionSelected;

    @Column(name = "dimension_definitions", columnDefinition = "json", nullable = false)
    @Comment("v4 분기항목 정의 (dimension definitions, v1-v3은 빈 배열)")
    private String dimensionDefinitions;

    @Column(name = "api_version", nullable = false, length = 20)
    @ColumnDefault("'v1'")
    @Comment("생성 API 버전 (v1~v4) - 확률 스케일 판정 기준 (v4만 0~1 비율, 2026-08-18 실측)")
    private String apiVersion;

    @Column(name = "scenario_count", nullable = false)
    @ColumnDefault("0")
    @Comment("시나리오 수")
    private Integer scenarioCount;

    @Convert(converter = ScenarioGroupStatusConverter.class)
    @Column(name = "status", nullable = false, length = 50)
    @ColumnDefault("'creating'")
    @Comment("상태 (creating:생성중 / created:생성완료 / realized:실현)")
    private ScenarioGroupStatus status;

    @Convert(converter = ScenarioGroupStageConverter.class)
    @Column(name = "stage", nullable = false, length = 50)
    @ColumnDefault("'ready'")
    @Comment("단계 (ready / scenario_created / scenario_validated / signpost_created / completed)")
    private ScenarioGroupStage stage;

    @Column(name = "request_usr_id", columnDefinition = "int unsigned")
    @Comment("요청자 사용자 ID")
    private Long requestUserId;

    /**
     * 시나리오 판정 결과로부터 그룹 status를 재도출한다. (불변식: 그룹 realized = 그룹 내 실현 시나리오 존재)
     * 실현을 되돌리면 created로 환원한다. 판정은 생성이 끝난 그룹에서만 일어나므로 creating으로는 돌아가지 않는다.
     */
    public void deriveStatus(boolean anyScenarioRealized) {
        this.status = anyScenarioRealized ? ScenarioGroupStatus.REALIZED : ScenarioGroupStatus.CREATED;
    }

    @Builder
    public MiScenarioGroup(Long scenarioGroupId, String title,
                           String summary, String description, String dimensionSelected, Integer scenarioCount,
                           ScenarioGroupStatus status, ScenarioGroupStage stage, Long requestUserId) {
        this.scenarioGroupId = scenarioGroupId;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.dimensionSelected = dimensionSelected;
        this.scenarioCount = scenarioCount;
        this.status = status;
        this.stage = stage;
        this.requestUserId = requestUserId;
    }

}

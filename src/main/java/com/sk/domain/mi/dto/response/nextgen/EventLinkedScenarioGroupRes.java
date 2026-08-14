package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 이벤트에 연결된 시나리오 그룹 (이벤트 목록의 그룹 N 펼침 패널 한 행).
 */
public record EventLinkedScenarioGroupRes(

    @Schema(description = "시나리오 그룹 ID", example = "410")
    Long scenarioGroupId,

    @Schema(description = "시나리오 그룹명", example = "시나리오 그룹명 01 - 카타르 LNG 공급 차질 확산")
    String title,

    @Schema(description = "그룹 상태 (creating/created/realized)", example = "creating")
    ScenarioGroupStatus status,

    @Schema(description = "그룹 상태 라벨", example = "생성중")
    String statusLabel,

    @Schema(description = "설계 단계 (ready/scenario_created/scenario_validated/signpost_created/completed)", example = "scenario_created")
    ScenarioGroupStage stage,

    @Schema(description = "설계 단계 라벨", example = "시나리오 생성완료")
    String stageLabel,

    @Schema(description = "설계중 세부 진행 단계 (step1/step2) - 상태 배지 1단계/2단계 표기용. 설계중이 아니면 null", example = "step1", nullable = true)
    ScenarioDesignDisplayStatus.CreatingStep designStep,

    @Schema(description = "설계중 세부 진행 단계 라벨. 설계중이 아니면 null", example = "분기항목 설계", nullable = true)
    String designStepLabel,

    @Schema(description = "그룹을 구성하는 이벤트명 목록 (표시 순서)")
    List<String> events

) {

    public static EventLinkedScenarioGroupRes of(Long scenarioGroupId, String title,
                                                 ScenarioGroupStatus status, ScenarioGroupStage stage,
                                                 List<String> events) {
        ScenarioDesignDisplayStatus.CreatingStep designStep =
            status == ScenarioGroupStatus.CREATING ? ScenarioDesignDisplayStatus.CreatingStep.of(stage) : null;
        return new EventLinkedScenarioGroupRes(
            scenarioGroupId,
            title,
            status,
            status == null ? null : status.getLabel(),
            stage,
            stage == null ? null : stage.getLabel(),
            designStep,
            designStep == null ? null : designStep.getLabel(),
            events
        );
    }

}

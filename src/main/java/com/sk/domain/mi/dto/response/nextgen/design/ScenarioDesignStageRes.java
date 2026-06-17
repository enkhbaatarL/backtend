package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 설계 단계 전이 응답 (설계 시작 / 검토 요청 / 단계2 진입 / 최종 확정 공통).
 */
public record ScenarioDesignStageRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "그룹 상태 (creating/created/realized)", example = "creating")
    ScenarioGroupStatus status,

    @Schema(description = "그룹 상태 라벨", example = "생성중")
    String statusLabel,

    @Schema(description = "설계 단계 (ready/scenario_created/scenario_validated/signpost_created/completed)", example = "scenario_created")
    ScenarioGroupStage stage,

    @Schema(description = "설계 단계 라벨", example = "시나리오 생성완료")
    String stageLabel

) {

    public static ScenarioDesignStageRes of(Long scenarioGroupId, ScenarioGroupStatus status, ScenarioGroupStage stage) {
        return new ScenarioDesignStageRes(
            scenarioGroupId,
            status,
            status == null ? null : status.getLabel(),
            stage,
            stage == null ? null : stage.getLabel()
        );
    }

}

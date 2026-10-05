package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 이벤트 조합 설계 상태 — 선택한 이벤트 조합과 구성이 같은 시나리오 그룹 목록
 */
public record ScenarioDesignStatusRes(

    @Schema(description = "구성이 같은 그룹 수", example = "3")
    int sameCombinationGroupCount,

    @Schema(description = "구성이 같은 그룹 목록 (생성일시 최신순)")
    List<SameCombinationGroup> sameCombinationGroups

) {

    public static ScenarioDesignStatusRes of(List<SameCombinationGroup> groups) {
        return new ScenarioDesignStatusRes(groups.size(), groups);
    }

    /**
     * 구성이 같은 그룹 (동일 조합 그룹 목록 한 행)
     */
    public record SameCombinationGroup(

        @Schema(description = "시나리오 그룹 ID", example = "410")
        Long scenarioGroupId,

        @Schema(description = "시나리오 그룹명", example = "추가 가격 · Force Majeure 확대 Tail Risk 시나리오")
        String title,

        @Schema(description = "생성자명", example = "홍길동", nullable = true)
        String creatorName,

        @Schema(description = "생성일시", example = "2026-08-27 15:52:00")
        LocalDateTime createdAt,

        @Schema(description = "그룹 상태 (creating/created/realized)", example = "creating")
        ScenarioGroupStatus status,

        @Schema(description = "그룹 상태 라벨", example = "생성중")
        String statusLabel,

        @Schema(description = "설계 단계 (ready/scenario_created/scenario_validated/signpost_created/completed)", example = "scenario_created")
        ScenarioGroupStage stage,

        @Schema(description = "설계 단계 라벨", example = "시나리오 생성완료")
        String stageLabel,

        @Schema(description = "설계중 세부 진행 단계 (step1/step2). 설계중이 아니면 null", example = "step1", nullable = true)
        ScenarioDesignDisplayStatus.CreatingStep designStep,

        @Schema(description = "설계중 세부 진행 단계 라벨. 설계중이 아니면 null", example = "분기항목 설계", nullable = true)
        String designStepLabel,

        @Schema(description = "설계중 그룹의 진행자가 로그인 사용자인지. 설계중이 아니면 null", example = "true", nullable = true)
        Boolean mine

    ) {

        public static SameCombinationGroup of(Long scenarioGroupId, String title, String creatorName, LocalDateTime createdAt,
                                              ScenarioGroupStatus status, ScenarioGroupStage stage, Boolean mine) {
            boolean creating = status == ScenarioGroupStatus.CREATING;
            ScenarioDesignDisplayStatus.CreatingStep designStep = creating ? ScenarioDesignDisplayStatus.CreatingStep.of(stage) : null;
            return new SameCombinationGroup(
                scenarioGroupId,
                title,
                creatorName,
                createdAt,
                status,
                status == null ? null : status.getLabel(),
                stage,
                stage == null ? null : stage.getLabel(),
                designStep,
                designStep == null ? null : designStep.getLabel(),
                creating ? mine : null
            );
        }

    }

}

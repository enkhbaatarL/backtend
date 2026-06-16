package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 설계 세션 상태 조회 응답
 */
public record ScenarioDesignStateRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "그룹 상태 (creating/created/realized)", example = "creating")
    ScenarioGroupStatus status,

    @Schema(description = "그룹 상태 라벨", example = "생성중")
    String statusLabel,

    @Schema(description = "설계 단계 (ready/scenario_created/scenario_validated/signpost_created/completed)", example = "scenario_created")
    ScenarioGroupStage stage,

    @Schema(description = "설계 단계 라벨", example = "시나리오 생성완료")
    String stageLabel,

    @Schema(description = "점유 여부", example = "true")
    boolean occupied,

    @Schema(description = "점유자 사용자 ID", example = "1001")
    Long occupantUserId,

    @Schema(description = "단계1 표 (분기항목/시나리오). 해당 단계 아니면 null")
    DesignTable step1,

    @Schema(description = "단계2 One-view (그룹 메타 + Signpost 표). 해당 단계 아니면 null")
    OneView step2,

    @Schema(description = "검토 결과 (미수행 시 null)")
    Validation validation

) {

    /** 단계2 One-view (그룹명/요약/설명 + 시나리오 표). */
    public record OneView(

        @Schema(description = "시나리오 그룹명", example = "호르무즈 봉쇄 + 전면전")
        String groupName,

        @Schema(description = "시나리오 그룹 요약")
        String summary,

        @Schema(description = "시나리오 그룹 설명")
        String description,

        @Schema(description = "시나리오 표 (Signpost 포함)")
        DesignTable table

    ) {
        public static OneView of(String groupName, String summary, String description, DesignTable table) {
            return new OneView(groupName, summary, description, table);
        }
    }

    /** 검토 결과 (검토 미수행 시 null). */
    public record Validation(

        @Schema(description = "정합성 통과 여부", example = "true")
        Boolean isValid,

        @Schema(description = "검토 사유/코멘트")
        String reason

    ) {
        public static Validation of(Boolean isValid, String reason) {
            return new Validation(isValid, reason);
        }
    }

    public static ScenarioDesignStateRes of(Long scenarioGroupId, ScenarioGroupStatus status, ScenarioGroupStage stage,
                                            boolean occupied, Long occupantUserId,
                                            DesignTable step1, OneView step2, Validation validation) {
        return new ScenarioDesignStateRes(
            scenarioGroupId,
            status,
            status == null ? null : status.getLabel(),
            stage,
            stage == null ? null : stage.getLabel(),
            occupied,
            occupantUserId,
            step1,
            step2,
            validation
        );
    }
}

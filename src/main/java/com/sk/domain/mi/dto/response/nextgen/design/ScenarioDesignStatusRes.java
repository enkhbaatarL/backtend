package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이벤트 조합 설계 상태
 */
public record ScenarioDesignStatusRes(

    @Schema(description = "조합 설계 상태 - 플로팅바 CTA 분기 기준", example = "designable")
    ScenarioDesignDisplayStatus designStatus,

    @Schema(description = "조합 설계 상태 라벨", example = "설계가능")
    String designStatusLabel,

    @Schema(description = "시나리오 그룹 ID (DESIGNABLE 일 때, null)", example = "410", nullable = true)
    Long scenarioGroupId,

    @Schema(description = "설계중 세부 진행 단계 (step1/step2)", example = "step1", nullable = true)
    ScenarioDesignDisplayStatus.CreatingStep designStep,

    @Schema(description = "설계중 세부 진행 단계 라벨. 그 외 null", example = "분기항목 설계", nullable = true)
    String designStepLabel,

    @Schema(description = "설계 점유 여부 (설계중이면 true)", example = "false")
    boolean occupied,

    @Schema(description = "점유 사용자 ID (점유 중이 아니면 null)", example = "10001", nullable = true)
    Long occupantUserId,

    @Schema(description = "점유 사용자명 (점유 중이 아니면 null)", example = "홍길동", nullable = true)
    String occupantUserName,

    @Schema(description = "내 진행 여부 (CREATING 일 때, true면 단계 이동, false면 잠금) 그 외 null", example = "true", nullable = true)
    Boolean mine

) {

    /**
     * 설계중
     * */
    public static ScenarioDesignStatusRes creating(Long scenarioGroupId, ScenarioGroupStage stage, Long occupantUserId, String occupantUserName, boolean mine) {
        ScenarioDesignDisplayStatus.CreatingStep step = ScenarioDesignDisplayStatus.CreatingStep.of(stage);
        return new ScenarioDesignStatusRes(
            ScenarioDesignDisplayStatus.CREATING,
            ScenarioDesignDisplayStatus.CREATING.getLabel(),
            scenarioGroupId,
            step,
            step == null ? null : step.getLabel(),
            true,
            occupantUserId,
            occupantUserName,
            mine
        );
    }

    /**
     * 설계완료
     * */
    public static ScenarioDesignStatusRes created(Long scenarioGroupId) {
        return new ScenarioDesignStatusRes(
            ScenarioDesignDisplayStatus.CREATED,
            ScenarioDesignDisplayStatus.CREATED.getLabel(),
            scenarioGroupId,
            null, null, false, null, null, null
        );
    }

    /**
     * 설계가능
     * */
    public static ScenarioDesignStatusRes designable() {
        return new ScenarioDesignStatusRes(
            ScenarioDesignDisplayStatus.DESIGNABLE,
            ScenarioDesignDisplayStatus.DESIGNABLE.getLabel(),
            null, null, null, false, null, null, null
        );
    }

}

package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.shared.enums.nextgen.ScenarioDesignDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 이벤트별 시나리오 설계 점유/가능 상태 (이벤트 목록 폴링).
 */
public record EventDesignAvailabilityRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "설계 상태 (designable/unavailable/creating/created)", example = "designable")
    ScenarioDesignDisplayStatus scenarioDesignStatus,

    @Schema(description = "설계 상태 라벨", example = "설계가능")
    String scenarioDesignStatusLabel,

    @Schema(description = "설계중(creating) 세부 진행 단계 (step1/step2). 설계중이 아니면 null", example = "step1", nullable = true)
    ScenarioDesignDisplayStatus.CreatingStep designStep,

    @Schema(description = "설계중 세부 진행 단계 라벨 (분기항목 설계/Signpost 설계). 설계중이 아니면 null", example = "분기항목 설계", nullable = true)
    String designStepLabel,

    @Schema(description = "점유 여부 (설계 진행 중이면 true)", example = "false")
    boolean occupied,

    @Schema(description = "점유 사용자 ID (점유 중이 아니면 null)", example = "10001")
    Long occupantUserId,

    @Schema(description = "점유 사용자명 (점유 중이 아니면 null)", example = "홍길동")
    String occupantUserName,

    @Schema(description = "활성 시나리오 그룹 ID (없으면 null)", example = "900001")
    Long scenarioGroupId

) {

    public static EventDesignAvailabilityRes of(Long eventId, ScenarioDesignDisplayStatus status, ScenarioDesignDisplayStatus.CreatingStep designStep,
                                                boolean occupied, Long occupantUserId, String occupantUserName, Long scenarioGroupId) {
        return new EventDesignAvailabilityRes(eventId, status, status == null ? null : status.getLabel(),
            designStep, designStep == null ? null : designStep.getLabel(),
            occupied, occupantUserId, occupantUserName, scenarioGroupId);
    }

    /** 설계 세션이 없는(설계 가능) 이벤트. */
    public static EventDesignAvailabilityRes designable(Long eventId) {
        return of(eventId, ScenarioDesignDisplayStatus.DESIGNABLE, null, false, null, null, null);
    }

}

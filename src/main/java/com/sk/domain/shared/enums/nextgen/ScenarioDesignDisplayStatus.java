package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이벤트 관점의 시나리오 디자인 가능 상태
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioDesignDisplayStatus {

    DESIGNABLE("designable", "설계가능"),
    DESIGN_UNAVAILABLE("unavailable", "설계불가"),
    CREATING("creating", "설계중"),
    CREATED("created", "설계완료");

    @JsonValue
    private final String code;
    private final String label;

    /**
     * 활성 그룹이 없으면 dimension template 유무로 설계가능/설계불가를 가르고, 있으면 그룹 status 기준 분류.
     * - 그룹 없음 + 템플릿 있음 --> 설계가능 / 그룹 없음 + 템플릿 없음 --> 설계불가 (해당 event_type_l2에 dimension template 미존재)
     * - creating/created/realized는 이미 설계가 진행 or 완료된 상태라 템플릿 유무와 무관하게 유지
     *
     * @param templateAvailable 이벤트의 event_type_l2에 매칭되는 비삭제 dimension template 존재 여부
     */
    public static ScenarioDesignDisplayStatus of(ScenarioGroupStatus groupStatus, boolean templateAvailable) {
        if (groupStatus == null) {
            return templateAvailable ? DESIGNABLE : DESIGN_UNAVAILABLE;
        }
        return switch (groupStatus) {
            case CREATING -> CREATING;
            case CREATED, REALIZED -> CREATED;
            default -> throw new IllegalStateException("지원하지 않는 ScenarioGroupStatus 값입니다: " + groupStatus);
        };
    }

    @JsonCreator
    public static ScenarioDesignDisplayStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioDesignDisplayStatus: " + value));
    }

    /**
     * CREATING(설계중)의 세부 진행 단계. 그룹 stage에서 파생하며, 설계중이 아니면 null.
     * step1 = 분기항목 설계 (ready / scenario_created / scenario_validated)
     * step2 = Signpost 설계 (signpost_created)
     */
    @Getter
    @RequiredArgsConstructor
    public enum CreatingStep {

        STEP1("step1", "분기항목 설계"),
        STEP2("step2", "Signpost 설계");

        @JsonValue
        private final String code;
        private final String label;

        public static CreatingStep of(ScenarioGroupStage stage) {
            if (stage == null) return null;
            return switch (stage) {
                case READY, SCENARIO_CREATED, SCENARIO_VALIDATED -> STEP1;
                case SIGNPOST_CREATED -> STEP2;
                case COMPLETED -> null; // 확정완료 = 설계중 아님
            };
        }

        @JsonCreator
        public static CreatingStep from(String value) {
            return Arrays.stream(values())
                .filter(step -> step.name().equalsIgnoreCase(value) || step.code.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown CreatingStep: " + value));
        }

    }

}

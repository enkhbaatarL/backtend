package com.sk.domain.mi.dto.response.nextgen.design;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 설계 세션 SSE 푸시 이벤트 (신호).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScenarioDesignSseEvent(

    Long scenarioGroupId,
    Type type,
    Stage stage,
    AgentCallbackResult result,
    String reason

) {

    /**
     * 설계 파이프라인 콜백 트리거 단계 (SSE 이벤트 종류).
     * 콜백 3종(생성/검토/Signpost)과 1:1 대응한다.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Type {

        GENERATION("generation"),
        VALIDATION("validation"),
        SIGNPOSTS("signposts");

        @JsonValue
        private final String code;

        @JsonCreator
        public static Type from(String value) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value) || type.code.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioDesignSseEvent.Type: " + value));
        }
    }

    /**
     * 콜백으로 전이되는 설계 stage.
     * 그룹 stage 체계와 값이 일부 다르므로(겹치지 않는 상태값 존재) 설계 전용으로 분리한다.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Stage {

        SCENARIO_CREATED("scenario_created"),
        SCENARIO_VALIDATED("scenario_validated"),
        SIGNPOST_CREATED("signpost_created");

        @JsonValue
        private final String code;

        @JsonCreator
        public static Stage from(String value) {
            return Arrays.stream(values())
                .filter(stage -> stage.name().equalsIgnoreCase(value) || stage.code.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioDesignSseEvent.Stage: " + value));
        }
    }

    /** 성공 이벤트. result=success, reason 없음. */
    public static ScenarioDesignSseEvent of(Long scenarioGroupId, Type type, Stage stage) {
        return new ScenarioDesignSseEvent(scenarioGroupId, type, stage, AgentCallbackResult.SUCCESS, null);
    }

    /** 실패 이벤트 (Agent 작업 실패). 단계 전이 없이 사유만 전달 */
    public static ScenarioDesignSseEvent failure(Long scenarioGroupId, Type type, String reason) {
        return new ScenarioDesignSseEvent(scenarioGroupId, type, null, AgentCallbackResult.FAILURE, reason);
    }

}

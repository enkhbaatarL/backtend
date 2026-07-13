package com.sk.domain.mi.dto.response.nextgen.priceinterp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 가격전망 해석 SSE 푸시 이벤트 (신호)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PriceInterpretationSseEvent(

    Long scenarioGroupId,
    Type type,
    AgentCallbackResult result,
    String reason

) {

    /** SSE 이벤트 종류. */
    @Getter
    @RequiredArgsConstructor
    public enum Type {

        GENERATION("generation");

        @JsonValue
        private final String code;

        @JsonCreator
        public static Type from(String value) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value) || type.code.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PriceInterpretationSseEvent.Type: %s".formatted(value)));
        }
    }

    public static PriceInterpretationSseEvent of(Long scenarioGroupId, Type type) {
        return new PriceInterpretationSseEvent(scenarioGroupId, type, AgentCallbackResult.SUCCESS, null);
    }

    public static PriceInterpretationSseEvent failure(Long scenarioGroupId, Type type, String reason) {
        return new PriceInterpretationSseEvent(scenarioGroupId, type, AgentCallbackResult.FAILURE, reason);
    }

}

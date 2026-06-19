package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Set;
import lombok.Getter;

/**
 * Agent 콜백의 작업 성공 여부
 * <p>실패는 Agent가 {@code "failure"} 또는 {@code "error"}로 보낼 수 있어 둘 다 {@link #FAILURE}로 수용한다.
 */
@Getter
public enum AgentCallbackResult {

    SUCCESS("success"),
    FAILURE("failure", "error");

    @JsonValue
    private final String code;

    /** 역직렬화 시 code 외에 추가로 허용하는 입력 별칭. 직렬화(@JsonValue)는 code로만 한다. */
    private final Set<String> inboundAliases;

    AgentCallbackResult(String code, String... inboundAliases) {
        this.code = code;
        this.inboundAliases = Set.of(inboundAliases);
    }

    @JsonCreator
    public static AgentCallbackResult from(String value) {
        return Arrays.stream(values())
            .filter(result -> result.matches(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown AgentCallbackResult: " + value));
    }

    private boolean matches(String value) {
        if (name().equalsIgnoreCase(value) || code.equalsIgnoreCase(value)) {
            return true;
        }
        return inboundAliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(value));
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

}

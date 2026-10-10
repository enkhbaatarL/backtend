package com.sk.domain.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackTypeCd {

    AGENT_OUTPUT("agent_output", "Agent Output"),
    UX_UI("ux_ui", "UX/UI"),
    FEATURE_SUGGESTION("feature_suggestion", "기능 제안"),
    ETC("etc", "기타");

    @JsonValue
    private final String code;
    private final String description;

    @JsonCreator
    public static FeedbackTypeCd fromValue(String value) {
        for (FeedbackTypeCd type : values()) {
            if (type.code.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown feedback type: " + value);
    }

    /**
     * [MCP Feedback] 대소문자·공백·하이픈·밑줄과 별칭을 허용해 유형을 찾는다. 없거나 모르는 값이면 ETC.
     */
    public static FeedbackTypeCd fromLenient(String value) {
        String key = value == null ? "" : value.replaceAll("[\\s_\\-/]", "").toLowerCase(Locale.ROOT);
        for (FeedbackTypeCd type : values()) {
            if (type.code.replace("_", "").equals(key) || type.description.replaceAll("[\\s/]", "").toLowerCase(Locale.ROOT).equals(key)) {
                return type;
            }
        }
        // LLM·사용자가 흔히 쓰는 짧은 표현
        return switch (key) {
            case "agent", "output", "ai", "answer" -> AGENT_OUTPUT;
            case "ux", "ui" -> UX_UI;
            case "feature", "suggestion", "기능", "제안" -> FEATURE_SUGGESTION;
            default -> ETC;
        };
    }

}

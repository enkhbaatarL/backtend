package com.sk.domain.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackTypeCd {

    DATA_ERROR("data_error", "데이터 오류"),
    UI_IMPROVEMENT("ui_improvement", "UI 개선"),
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

}

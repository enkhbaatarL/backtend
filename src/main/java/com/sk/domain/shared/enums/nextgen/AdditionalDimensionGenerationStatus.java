package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 추가 분기항목 후보의 background 생성 상태
 */
@Getter
@RequiredArgsConstructor
public enum AdditionalDimensionGenerationStatus {

    NOT_REQUESTED("not_requested", "미시작"),
    QUEUED("queued", "대기중"),
    RUNNING("running", "생성중"),
    READY("ready", "준비완료"),
    FAILED("failed", "실패"),
    INTERRUPTED("interrupted", "중단"),
    DISCARDED("discarded", "폐기");

    @JsonValue
    private final String code;
    private final String label;

    public static AdditionalDimensionGenerationStatus fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }

}

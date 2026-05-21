package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelCaseStatus {

    PENDING("PENDING", "실행 대기"),
    RUNNING("RUNNING", "실행 중"),
    COMPLETED("COMPLETED", "완료"),
    FAILED("FAILED", "실패");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static FundamentalModelCaseStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelCaseStatus: " + value));
    }
}

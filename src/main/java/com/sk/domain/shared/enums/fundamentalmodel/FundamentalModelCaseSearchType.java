package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelCaseSearchType {

    TITLE("TITLE", "제목"),
    ASSIGNEE_NAME("ASSIGNEE_NAME", "담당자명");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static FundamentalModelCaseSearchType from(String value) {
        return Arrays.stream(values())
            .filter(type -> type.name().equalsIgnoreCase(value) || type.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelCaseSearchType: " + value));
    }
}

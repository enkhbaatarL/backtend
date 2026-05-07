package com.sk.domain.shared.enums.scenarioworkbench;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortOrder {

    LATEST("latest", "최신순"),
    OLDEST("oldest", "오래된순");

    @JsonValue
    private final String code;
    private final String label;

    public boolean isAscending() {
        return this == OLDEST;
    }

}

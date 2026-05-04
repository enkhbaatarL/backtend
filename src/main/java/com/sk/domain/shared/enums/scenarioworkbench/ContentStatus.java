package com.sk.domain.shared.enums.scenarioworkbench;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentStatus {

    ALL("ALL", "전체"),
    NEW("NEW", "신규"),
    UPDATED("UPDATED", "갱신");

    @JsonValue
    private final String code;
    private final String label;

    public boolean isNew() {
        return this == NEW;
    }

    public boolean isUpdated() {
        return this == UPDATED;
    }

}

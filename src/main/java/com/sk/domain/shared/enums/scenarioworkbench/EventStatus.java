package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventStatus {

    ALL("전체"),
    NEW("신규"),
    UPDATED("갱신");

    private final String label;

    public boolean isNew() {
        return this == NEW;
    }

    public boolean isUpdated() {
        return this == UPDATED;
    }
}

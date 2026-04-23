package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventStatus {
    NEW("NEW", "신규"),
    UPDATED("UPDATED", "갱신"),
    CLOSED("CLOSED", "종료");

    private final String code;
    private final String description;

    public boolean isActive() {
        return this == NEW || this == UPDATED;
    }
}

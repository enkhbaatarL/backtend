package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventSignalType {
    SIGNAL("Signal", "신호"),
    MIXED("Mixed", "혼합"),
    NOISE("Noise", "소음");

    private final String code;
    private final String description;
}

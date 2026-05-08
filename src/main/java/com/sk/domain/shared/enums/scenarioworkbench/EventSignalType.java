package com.sk.domain.shared.enums.scenarioworkbench;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventSignalType {

    SIGNAL("signal", "신호"),
    NOISE("noise", "소음");

    @JsonValue
    private final String code;
    private final String label;

}

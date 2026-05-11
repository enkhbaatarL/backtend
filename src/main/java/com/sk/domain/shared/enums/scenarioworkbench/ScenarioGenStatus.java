package com.sk.domain.shared.enums.scenarioworkbench;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScenarioGenStatus {

    READY("READY", "생성준비"),
    PROCESSING("PROCESSING", "생성중"),
    COMPLETED("COMPLETED", "생성완료");

    @JsonValue
    private final String code;
    private final String label;

}

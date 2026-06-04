package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScenarioGenStatus {

    READY("ready", "생성준비"),
    PROCESSING("processing", "생성중"),
    COMPLETED("completed", "생성완료");

    @JsonValue
    private final String code;
    private final String label;

}

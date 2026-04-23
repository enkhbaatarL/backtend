package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventSort {
    LATEST("LATEST", "최신순"),
    OLDEST("OLDEST", "오래된순");

    private final String code;
    private final String description;
}

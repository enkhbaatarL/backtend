package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketDirection {
    UP("UP", "상승"),
    FLAT("FLAT", "보합"),
    DOWN("DOWN", "하락"),
    NONE("NONE", "없음");

    private final String code;
    private final String description;
}

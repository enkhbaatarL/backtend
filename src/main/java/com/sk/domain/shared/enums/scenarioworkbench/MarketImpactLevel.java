package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketImpactLevel {
    HIGH("HIGH", "높음"),
    MEDIUM("MEDIUM", "중간"),
    LOW("LOW", "낮음"),
    NONE("NONE", "없음");

    private final String code;
    private final String description;
}

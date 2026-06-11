package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 그룹 상태 (tmm_mi_scenario_group_m.status).
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioGroupStatus {

    CREATING("creating", "생성중"),
    CREATED("created", "생성완료"),
    REALIZED("realized", "실현");

    private final String code;
    private final String label;

    public static ScenarioGroupStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioGroupStatus: " + value));
    }

}

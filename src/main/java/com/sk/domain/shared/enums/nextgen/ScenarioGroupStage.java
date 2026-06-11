package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 그룹 생성 파이프라인 단계 (tmm_mi_scenario_group_m.stage).
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioGroupStage {

    READY("ready", "준비"),
    SCENARIO_CREATED("scenario_created", "시나리오 생성완료"),
    SCENARIO_VALIDATED("scenario_validated", "시나리오 검증완료"),
    SIGNPOST_CREATED("signpost_created", "Signpost 생성완료"),
    COMPLETED("completed", "확정완료");

    private final String code;
    private final String label;

    public static ScenarioGroupStage from(String value) {
        return Arrays.stream(values())
            .filter(stage -> stage.name().equalsIgnoreCase(value) || stage.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioGroupStage: " + value));
    }

}

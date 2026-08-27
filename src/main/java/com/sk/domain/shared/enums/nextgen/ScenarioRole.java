package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 상대 역할 (tmm_mi_scenario_m.scenario_role, v4 전용).
 * 그룹 내에서 가격 전개의 상단(ceiling)/하단(floor)/혼합(mixed)을 대표하는 시나리오 표기.
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioRole {

    FLOOR("floor", "하단"),
    CEILING("ceiling", "상단"),
    MIXED("mixed", "혼합");

    @JsonValue
    private final String code;
    private final String label;

    /** Agent 응답 문자열 파싱. 미지 값은 null (Agent 어휘 확장 내성). */
    @JsonCreator
    public static ScenarioRole fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(role -> role.name().equalsIgnoreCase(value) || role.code.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }

}

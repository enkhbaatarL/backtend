package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 그룹 생성 API 버전 (tmm_mi_scenario_group_m.api_version).
 * v1~v3는 단일 이벤트 template 세대, v4는 멀티 이벤트 dimension_definitions 세대다.
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioApiVersion {

    V1("v1"),
    V2("v2"),
    V3("v3"),
    V4("v4");

    @JsonValue
    private final String code;

    @JsonCreator
    public static ScenarioApiVersion from(String value) {
        return Arrays.stream(values())
            .filter(version -> version.name().equalsIgnoreCase(value) || version.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioApiVersion: " + value));
    }

}

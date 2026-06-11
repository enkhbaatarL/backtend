package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이벤트 관점의 시나리오 디자인 가능 상태
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioDesignDisplayStatus {

    DESIGNABLE("designable", "설계가능"),
    CREATING("creating", "설계중"),
    CREATED("created", "설계완료");

    @JsonValue
    private final String code;
    private final String label;

    /**
     * 활성 그룹이 없으면 "설계가능", 있으면 그룹 status 기준 분류.
     * */
    public static ScenarioDesignDisplayStatus of(ScenarioGroupStatus groupStatus) {
        if (groupStatus == null) {
            return DESIGNABLE;
        }
        return switch (groupStatus) {
            case CREATING -> CREATING;
            case CREATED, REALIZED -> CREATED;
        };
    }

    @JsonCreator
    public static ScenarioDesignDisplayStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioDesignDisplayStatus: " + value));
    }

}

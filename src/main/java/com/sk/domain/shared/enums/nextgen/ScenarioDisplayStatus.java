package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 판정 상태
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioDisplayStatus {

    PENDING("pending", "대기", ScenarioStatus.CREATED),
    REALIZED("realized", "실현", ScenarioStatus.REALIZED),
    DISCARDED("discarded", "폐기", ScenarioStatus.DISCARDED);

    @JsonValue
    private final String code;
    private final String label;
    private final ScenarioStatus dbStatus;

    @JsonCreator
    public static ScenarioDisplayStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioDisplayStatus: " + value));
    }

    /**
     * DB status 값을 API enum으로 역매핑한다.
     * DB의 생성 중(creating)/완료(created) 상태는 외부 API의 pending 상태로 호환한다.
     */
    public static ScenarioDisplayStatus from(ScenarioStatus dbStatus) {
        if (dbStatus == null) {
            return PENDING;
        }
        return switch (dbStatus) {
            case CREATING, CREATED -> PENDING;
            case REALIZED -> REALIZED;
            case DISCARDED -> DISCARDED;
            default -> throw new IllegalStateException("지원하지 않는 ScenarioStatus 값입니다: " + dbStatus);
        };
    }

}

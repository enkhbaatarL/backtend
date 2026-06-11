package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 판정 상태 (가정 및 실행 가능성 표의 '상태'). 대기 → 실현/폐기.
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioStatus {

    PENDING("pending", "대기", "created"),
    REALIZED("realized", "실현", "realized"),
    DISCARDED("discarded", "폐기", "discarded");

    @JsonValue
    private final String code;
    private final String label;
    private final String dbStatus;

    @JsonCreator
    public static ScenarioStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioStatus: " + value));
    }

    /**
     * DB status 값을 API enum으로 역매핑한다.
     * DB의 생성 중/완료 상태는 외부 API의 pending 상태로 호환한다.
     */
    public static ScenarioStatus fromDbStatus(String dbStatus) {
        if (dbStatus == null || dbStatus.isBlank()) {
            return PENDING;
        }
        String normalized = dbStatus.trim();
        if ("creating".equalsIgnoreCase(normalized) || "created".equalsIgnoreCase(normalized)) {
            return PENDING;
        }
        return Arrays.stream(values())
            .filter(status -> status.dbStatus.equalsIgnoreCase(normalized))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown scenario DB status: " + dbStatus));
    }

    /**
     * @deprecated use {@link #fromDbStatus(String)} for tmm_mi_scenario_m.status.
     */
    @Deprecated(forRemoval = false)
    public static ScenarioStatus fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return PENDING;
        }
        return Arrays.stream(values())
            .filter(status -> status.label.equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ScenarioStatus label: " + label));
    }

}

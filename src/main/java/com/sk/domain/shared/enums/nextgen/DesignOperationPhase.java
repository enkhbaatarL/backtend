package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 설계 생성 작업의 처리 단계 (tmm_mi_scenario_group_operation_m.current_phase, Agent 소유 어휘).
 * event_analysis - dimension_design - scenario_synthesis - persistence 순으로 진행된다.
 */
@Getter
@RequiredArgsConstructor
public enum DesignOperationPhase {

    EVENT_ANALYSIS("event_analysis", "Event 분석"),
    DIMENSION_DESIGN("dimension_design", "분기항목 설계"),
    SCENARIO_SYNTHESIS("scenario_synthesis", "시나리오 합성"),
    PERSISTENCE("persistence", "결과 저장");

    @JsonValue
    private final String code;
    private final String label;

    /** Agent 응답 문자열 파싱. 미지 값은 null (Agent 어휘 확장 내성). */
    public static DesignOperationPhase fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(phase -> phase.name().equalsIgnoreCase(value) || phase.code.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }

}

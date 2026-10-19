package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Basecase Excel 행 스캔 종료 규칙 (tmm_mi_model_param_m.basecase_stop_rule).
 *
 * - STOP_WHEN_ALL_KEY_COLUMNS_EMPTY: key 컬럼이 모두 빈 첫 행에서 종료 (Agent의 Basecase 읽기와 동일)
 */
@Getter
@RequiredArgsConstructor
public enum BasecaseStopRule {

    STOP_WHEN_ALL_KEY_COLUMNS_EMPTY("stop_when_all_key_columns_empty", "key 컬럼이 모두 빈 첫 행에서 종료");

    private final String code;
    private final String label;

    public static BasecaseStopRule from(String value) {
        return Arrays.stream(values())
            .filter(rule -> rule.name().equalsIgnoreCase(value) || rule.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown BasecaseStopRule: " + value));
    }

}

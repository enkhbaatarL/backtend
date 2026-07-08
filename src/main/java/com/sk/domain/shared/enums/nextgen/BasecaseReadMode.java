package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Basecase Excel 읽기 방식 (tmm_mi_model_param_m.basecase_read_mode).
 *
 * - FIXED_COLUMNS: 행 단위 — slot/value가 고정 컬럼에 있는 형태
 * - MATRIX_TIME_COLUMNS: 월별 값이 열 방향으로 있는 matrix 형태
 *   (연/월/기간이 별도 행: basecase_year_row/month_row/period_row)
 */
@Getter
@RequiredArgsConstructor
public enum BasecaseReadMode {

    FIXED_COLUMNS("fixed_columns", "행 단위"),
    MATRIX_TIME_COLUMNS("matrix_time_columns", "월별 열 방향");

    private final String code;
    private final String label;

    public static BasecaseReadMode from(String value) {
        return Arrays.stream(values())
            .filter(mode -> mode.name().equalsIgnoreCase(value) || mode.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown BasecaseReadMode: " + value));
    }

}

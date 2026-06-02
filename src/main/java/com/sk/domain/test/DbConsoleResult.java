package com.sk.domain.test;

import java.util.List;
import java.util.Map;

/**
 * DB Console 실행 결과.
 *
 * @param mode         READ(조회) / WRITE(INSERT·UPDATE)
 * @param columns      READ 시 컬럼명 목록 (WRITE 시 null)
 * @param rows         READ 시 행 목록 (WRITE 시 null)
 * @param affectedRows WRITE 시 영향 행 수 (READ 시 null)
 * @param rowCount     READ=조회 행 수 / WRITE=영향 행 수
 * @param truncated    최대 행수 제한에 걸려 잘렸는지 여부
 * @param elapsedMs    실행 소요 시간(ms)
 */
public record DbConsoleResult(
    String mode,
    List<String> columns,
    List<Map<String, Object>> rows,
    Integer affectedRows,
    int rowCount,
    boolean truncated,
    long elapsedMs
) {
}

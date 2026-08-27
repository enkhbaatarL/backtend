package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 시나리오 설계 비동기 작업 상태
 */
@Getter
@RequiredArgsConstructor
public enum DesignOperationStatus {

    QUEUED("queued", "대기중"),
    RUNNING("running", "진행중"),
    SUCCEEDED("succeeded", "성공"),
    FAILED("failed", "실패"),
    INTERRUPTED("interrupted", "중단");

    @JsonValue
    private final String code;
    private final String label;

    /**
     * Agent 응답 문자열 파싱. 미지 값은 null (Agent 어휘 확장 내성).
     */
    public static DesignOperationStatus fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }

    /**
     * 진행중(재연결 대상) 상태 코드 목록 - queued/running.
     * DB 문자열 컬럼 조회 조건용.
     */
    public static List<String> activeCodes() {
        return List.of(QUEUED.code, RUNNING.code);
    }

    /**
     * 실패 종료 상태 코드 목록 - failed/interrupted.
     * DB 문자열 컬럼 조회 조건용 (좀비 그룹 판정 쿼리).
     */
    public static List<String> failedTerminalCodes() {
        return List.of(FAILED.code, INTERRUPTED.code);
    }

    /**
     * 실패 종료 여부 - failed/interrupted
     * (좀비 판정, lastError 합성 대상).
     */
    public boolean isFailedTerminal() {
        return this == FAILED || this == INTERRUPTED;
    }

    /**
     * 실패 종료 여부 판정
     * 문자열 상태를 파싱해 판정한다.
     */
    public static boolean isFailedTerminal(String value) {
        DesignOperationStatus status = fromNullable(value);
        return status != null && status.isFailedTerminal();
    }

}

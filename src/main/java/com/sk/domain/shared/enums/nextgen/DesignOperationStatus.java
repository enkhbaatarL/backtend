package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시나리오 설계 비동기 작업 상태 (tmm_mi_scenario_group_operation_m.status, Agent 소유 어휘).
 * 원본 상태의 적재·전이 주체는 Agent라서 응답에는 코드 문자열을 그대로 통과시키고,
 * 이 enum은 표시 라벨 파생 전용으로만 쓴다. 미지 값은 라벨 없이 코드만 내려간다.
 */
@Getter
@RequiredArgsConstructor
public enum DesignOperationStatus {

    QUEUED("queued", "대기중"),
    RUNNING("running", "진행중"),
    SUCCEEDED("succeeded", "성공"),
    FAILED("failed", "실패"),
    INTERRUPTED("interrupted", "중단");

    private final String code;
    private final String label;

    /** Agent 응답 문자열 파싱. 미지 값은 null (Agent 어휘 확장 내성). */
    public static DesignOperationStatus fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
            .filter(status -> status.name().equalsIgnoreCase(value) || status.code.equalsIgnoreCase(value))
            .findFirst()
            .orElse(null);
    }

    /** 표시 라벨 파생. 미지 어휘면 null — FE는 라벨 없으면 코드를 그대로 표시한다. */
    public static String labelOf(String value) {
        DesignOperationStatus status = fromNullable(value);
        return status == null ? null : status.label;
    }

}

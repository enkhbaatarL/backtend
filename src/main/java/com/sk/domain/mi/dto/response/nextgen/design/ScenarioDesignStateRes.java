package com.sk.domain.mi.dto.response.nextgen.design;

/**
 * 설계 세션 상태 (폴링).
 * 현재 stage에 따라 step1 / step2 중 해당분이 채워지고, 나머지는 null.
 */
public record ScenarioDesignStateRes(
    Long scenarioGroupId,
    String status,
    String stage,
    boolean occupied,
    Long occupantUserId,
    ScenarioDesignTable step1,
    OneView step2,
    Validation validation
) {

    /** 단계2 One-view (그룹명/요약/설명 + 시나리오 표). */
    public record OneView(
        String title,
        String summary,
        String description,
        ScenarioDesignTable table
    ) {}

    /** 검토 결과 (검토 미수행 시 null). */
    public record Validation(Boolean isValid, String reason) {}
}

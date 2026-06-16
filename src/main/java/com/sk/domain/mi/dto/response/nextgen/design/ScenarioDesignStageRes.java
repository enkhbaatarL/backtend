package com.sk.domain.mi.dto.response.nextgen.design;

/**
 * 설계 단계 전이 응답.
 * 설계 시작 / 검토 요청 / 단계2 진입 / 최종 확정 응답 공통.
 */
public record ScenarioDesignStageRes(
    Long scenarioGroupId,
    String status,
    String stage
) {}

package com.sk.domain.mi.dto.response.nextgen.design;

/**
 * 이벤트별 시나리오 설계 점유/가능 상태 (이벤트 목록 폴링).
 */
public record EventDesignAvailabilityRes(
    Long eventId,
    String scenarioDesignStatus,
    String scenarioDesignStatusLabel,
    boolean occupied,
    Long occupantUserId,
    Long scenarioGroupId
) {}

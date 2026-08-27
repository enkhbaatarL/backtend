package com.sk.domain.shared.client.dto.request;

import java.util.List;

/**
 * 멀티 이벤트 시나리오 그룹 생성 Agent 요청 (v4 SSE API).
 */
public record ScenarioGroupCreateV4AgentReq(
    List<Long> miEventIds,
    Long userId
) {

    public static ScenarioGroupCreateV4AgentReq of(List<Long> miEventIds, Long userId) {
        return new ScenarioGroupCreateV4AgentReq(miEventIds, userId);
    }

}

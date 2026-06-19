package com.sk.domain.shared.client.dto.request;

public record ScenarioGroupCreateAgentReq(
    Long miEventId,
    Long userId
) {

    public static ScenarioGroupCreateAgentReq of(Long miEventId, Long userId) {
        return new ScenarioGroupCreateAgentReq(miEventId, userId);
    }

}

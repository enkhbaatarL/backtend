package com.sk.domain.shared.client.dto.request;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;

public record ScenarioValidationAgentReq(
    Long userId,
    ScenarioDesignTable table
) {

    public static ScenarioValidationAgentReq of(Long userId, ScenarioDesignTable table) {
        return new ScenarioValidationAgentReq(userId, table);
    }

}

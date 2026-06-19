package com.sk.domain.shared.client.dto.request;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;

public record ScenarioSignpostsAgentReq(
    Long userId,
    ScenarioDesignTable table
) {

    public static ScenarioSignpostsAgentReq of(Long userId, ScenarioDesignTable table) {
        return new ScenarioSignpostsAgentReq(userId, table);
    }

}

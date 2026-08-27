package com.sk.domain.shared.client.dto.request;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;

public record ScenarioConfirmationAgentReq(
    Long userId,
    GroupEdit scenarioGroup,
    ScenarioDesignTable table
) {

    /** 그룹 편집 항목. */
    public record GroupEdit(
        String title,
        String summary,
        String description
    ) {}

    public static ScenarioConfirmationAgentReq of(Long userId, GroupEdit scenarioGroup, ScenarioDesignTable table) {
        return new ScenarioConfirmationAgentReq(userId, scenarioGroup, table);
    }

}

package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Agent 추가 분기항목 후보 조회 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdditionalDimensionsAgentRes(Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
        Long scenarioGroupId,
        String generationStatus,
        AgentDimensionDefinitions dimensionDefinitions,
        Integer availableCount,
        String updatedAt,
        String errorCode
    ) {
    }

}

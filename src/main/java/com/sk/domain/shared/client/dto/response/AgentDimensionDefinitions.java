package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Agent 분기항목 정의.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentDimensionDefinitions(
    String status,
    List<Item> items
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        String dimensionId,
        String label,
        String description,
        String rationale,
        String modelSupportStatus,
        List<Value> values
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
        String valueId,
        String label,
        String rationale,
        String pressureContribution
    ) {
    }

}

package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScenarioDesignAgentRes(

    String status,
    String message,
    Data data

) {

    public record Data(
        Long scenarioGroupId,
        String status,
        String stage,
        @JsonProperty("error_code")
        String errorCode,
        @JsonProperty("error_name")
        String errorName
    ) {
    }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

}

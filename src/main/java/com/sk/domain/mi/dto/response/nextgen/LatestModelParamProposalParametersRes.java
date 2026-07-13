package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

public record LatestModelParamProposalParametersRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "최신 Scenario Case ID", example = "88001")
    Long caseId,

    @Schema(description = "Backend execution-preparation ID", example = "51001")
    Long execPrepId,

    @Schema(description = "Agent model parameter proposal ID", example = "10001")
    Long modelParamProposalId,

    @Schema(description = "flat model parameter array")
    JsonNode parameters

) {
}

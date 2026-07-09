package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ModelParamProposalAcceptRes(

    @Schema(description = "Agent model parameter proposal ID", example = "10001")
    Long modelParamProposalId,

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "Agent proposal 접수 상태", implementation = AgentModelParamProposalStatus.class, example = "PROCESSING")
    AgentModelParamProposalStatus status,

    @Schema(description = "Backend proposal 요청 접수 시각")
    LocalDateTime requestedAt,

    @Schema(description = "상태/결과 조회 endpoint", example = "/api/v1/mi/next-gen/scenarios/309/model-param-proposals/10001")
    String statusEndpoint
) {
}

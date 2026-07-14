package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ModelParamParameterSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record LatestModelParamProposalParametersRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "최신 Scenario Case ID", example = "88001")
    Long caseId,

    @Schema(description = "Backend execution-preparation ID", example = "51001")
    Long execPrepId,

    @Schema(description = "Agent model parameter proposal ID", example = "10001")
    Long modelParamProposalId,

    @Schema(
        description = "반환 파라미터 출처",
        implementation = ModelParamParameterSource.class,
        example = "CONFIRMATION_PARAMETERS"
    )
    ModelParamParameterSource parameterSource,

    @Schema(
        description = "A4a와 동일한 modelFile > modelSheet > modelTable > parameter/rendering tree",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<CurrentModelParamFileRes> modelFiles

) {
}

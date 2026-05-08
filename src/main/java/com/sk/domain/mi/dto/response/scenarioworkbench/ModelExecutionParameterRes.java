package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ModelExecutionParameterRes(

    @Schema(description = "시나리오 ID", example = "1")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "호르무즈 봉쇄 3개월+ 유지")
    String title,

    @ArraySchema(schema = @Schema(implementation = ModelExecutionParameterTableRes.class))
    List<ModelExecutionParameterTableRes> tables

) {
}

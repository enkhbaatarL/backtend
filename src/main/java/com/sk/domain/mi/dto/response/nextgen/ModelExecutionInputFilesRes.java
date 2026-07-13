package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ModelExecutionInputFilesRes(

    @Schema(description = "DEMAND/SUPPLY/TRADE role이 각각 정확히 1개인지 여부", example = "true")
    boolean complete,

    @ArraySchema(schema = @Schema(
        description = "누락 파일 role",
        allowableValues = {"DEMAND", "SUPPLY", "TRADE"}
    ))
    List<String> missingFileRoles,

    @ArraySchema(schema = @Schema(implementation = ModelExecutionInputFileRes.class))
    List<ModelExecutionInputFileRes> files

) {
}

package com.sk.domain.mi.dto.request.nextgen;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ModelExecutionSubmitReq(

    @NotEmpty(message = "tables는 최소 1건 이상이어야 합니다")
    @Valid
    @ArraySchema(schema = @Schema(implementation = ModelExecutionTableReq.class))
    List<ModelExecutionTableReq> tables

) {
}

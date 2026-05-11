package com.sk.domain.mi.dto.request.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ScenarioGenReq(

    @NotEmpty(message = "eventIds는 필수입니다")
    @ArraySchema(
        arraySchema = @Schema(description = "대상 이벤트 ID 목록 (Phase 1: 1건, Phase 2: N건)"),
        schema = @Schema(type = "integer", example = "1")
    )
    List<Long> eventIds

) {
}

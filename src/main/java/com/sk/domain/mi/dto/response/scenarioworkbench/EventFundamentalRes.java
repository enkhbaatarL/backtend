package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record EventFundamentalRes(

    @ArraySchema(
        arraySchema = @Schema(description = "Fundamental 항목명 목록"),
        schema = @Schema(description = "항목명", example = "EU 총 저장 용량")
    )
    List<String> items

) {
}

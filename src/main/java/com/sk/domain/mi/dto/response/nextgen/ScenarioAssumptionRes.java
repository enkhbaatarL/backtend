package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record ScenarioAssumptionRes(

    @Schema(description = "가정 항목명", example = "갈등 기간")
    String label,

    @Schema(description = "가정 값", example = "6개월 이상")
    String value

) {
}

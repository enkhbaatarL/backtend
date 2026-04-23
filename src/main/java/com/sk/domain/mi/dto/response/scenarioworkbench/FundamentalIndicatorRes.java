package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalIndicatorRes(

    @Schema(description = "지표명", example = "Q1 순인출 증가")
    String indicatorName,

    @Schema(description = "현재 값", example = "+50% YoY")
    String currentValue,

    @Schema(description = "시사점", example = "겨울 이후 재고 부담 확대")
    String implication,

    @Schema(description = "출처", example = "IEA Gas Market Report")
    String source

) {
}

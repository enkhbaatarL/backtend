package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalMetricRes(

    @Schema(description = "항목명", example = "EU 총 저장 용량")
    String metricName,

    @Schema(description = "현재 값", example = "약 1,100 TWh")
    String currentValue,

    @Schema(description = "해석 기준", example = "EU 전체 저장 capacity 기준")
    String interpretation,

    @Schema(description = "출처", example = "GIE")
    String source

) {
}

package com.sk.domain.mi.dto.response.forecast.summary;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Forecast record 테이블 헤더")
public record ForecastRecordHeaderRes(
    @Schema(description = "컬럼키", example = "FORECAST_VALUE")
    String columnKey,

    @Schema(description = "헤더 텍스트", example = "Forecast Value")
    String headerText,

    @Schema(description = "표시 여부", example = "true")
    Boolean isVisible,

    @Schema(description = "정렬 순서", example = "3")
    Integer orderNo
) {
}

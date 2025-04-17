package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.Interval;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChartMetadata(

    @Schema(description = "값 단위", example = "MMBtu")
    String unit,

    @Schema(description = "interval (DAY, WEEK, MONTH, YEAR)", example = "YEAR")
    Interval interval

) {
}

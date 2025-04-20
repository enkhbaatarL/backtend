package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.Interval;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ChartMetadata(

    @Schema(description = "값 단위", example = "MMBtu")
    String unit,

    @Schema(description = "interval (DAY, WEEK, MONTH, YEAR)", example = "YEAR")
    Interval interval

    // todo 변경할 수 있는 단위 리스트

) {

    @Builder
    public ChartMetadata(String unit, Interval interval) {
        this.unit = unit;
        this.interval = interval;
    }

    public static ChartMetadata of(String unit, Interval interval) {
        return ChartMetadata.builder()
            .unit(unit)
            .interval(interval)
            .build();
    }

}

package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.IndicatorType;
import lombok.Builder;

import java.util.List;

@Builder
public record Indicator(

    IndicatorType indicatorType,
    List<String> timestamps,
    List<Series> series

) {

    public static Indicator of(IndicatorType indicatorType, List<String> timestamps, List<Series> series) {
        return Indicator.builder()
            .indicatorType(indicatorType)
            .timestamps(timestamps)
            .series(series)
            .build();
    }

}

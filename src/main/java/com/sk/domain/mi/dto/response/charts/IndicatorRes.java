package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.IndicatorType;
import lombok.Builder;

import java.util.List;

public record IndicatorRes(

    IndicatorType indicatorType,
    List<String> timestamps,
    List<Series> series

) {

    @Builder
    public IndicatorRes(IndicatorType indicatorType, List<String> timestamps, List<Series> series) {
        this.indicatorType = indicatorType;
        this.timestamps = timestamps;
        this.series = series;
    }

    public static IndicatorRes of(IndicatorType indicatorType, List<String> timestamps, List<Series> series) {
        return IndicatorRes.builder()
            .indicatorType(indicatorType)
            .timestamps(timestamps)
            .series(series)
            .build();
    }

}

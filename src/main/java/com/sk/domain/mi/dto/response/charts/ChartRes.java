package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.ChartType;
import lombok.Builder;

import java.util.List;

public record ChartRes(

    ChartType chartType,
    List<String> timestamps,
    List<Series> series

) {

    @Builder
    public ChartRes(ChartType chartType, List<String> timestamps, List<Series> series) {
        this.chartType = chartType;
        this.timestamps = timestamps;
        this.series = series;
    }

    public static ChartRes of(ChartType chartType, List<String> timestamps, List<Series> series) {
        return ChartRes.builder()
            .chartType(chartType)
            .timestamps(timestamps)
            .series(series)
            .build();
    }

}

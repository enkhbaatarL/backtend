package com.sk.domain.mi.dto.response.forecast.charts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChartRawData;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

public record ForecastDataCache(
        List<ForecastChartRawData> periodAverages,
        LocalDate lastUpdated,
        ForecastDataSource dataSource) {

    @Builder
    @JsonCreator
    public ForecastDataCache(List<ForecastChartRawData> periodAverages, LocalDate lastUpdated, ForecastDataSource dataSource) {
        this.periodAverages = periodAverages;
        this.lastUpdated = lastUpdated;
        this.dataSource = dataSource;
    }

    public static ForecastDataCache of(List<ForecastChartRawData> periodAverages, LocalDate lastUpdated, ForecastDataSource dataSource) {
        return ForecastDataCache.builder()
                .periodAverages(periodAverages)
                .lastUpdated(lastUpdated)
                .dataSource(dataSource)
                .build();
    }
}

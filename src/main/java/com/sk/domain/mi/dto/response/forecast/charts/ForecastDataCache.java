package com.sk.domain.mi.dto.response.forecast.charts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChartRawData;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

public record ForecastDataCache(
        List<ForecastChartRawData> monthlyAverages,
        LocalDate lastUpdated,
        ForecastDataSource dataSource) {

    @Builder
    @JsonCreator
    public ForecastDataCache(List<ForecastChartRawData> monthlyAverages, LocalDate lastUpdated, ForecastDataSource dataSource) {
        this.monthlyAverages = monthlyAverages;
        this.lastUpdated = lastUpdated;
        this.dataSource = dataSource;
    }

    public static ForecastDataCache of(List<ForecastChartRawData> monthlyAverages, LocalDate lastUpdated, ForecastDataSource dataSource) {
        return ForecastDataCache.builder()
                .monthlyAverages(monthlyAverages)
                .lastUpdated(lastUpdated)
                .dataSource(dataSource)
                .build();
    }
}

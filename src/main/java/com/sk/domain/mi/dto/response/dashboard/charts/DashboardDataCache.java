package com.sk.domain.mi.dto.response.dashboard.charts;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartRawData;

import lombok.Builder;

public record DashboardDataCache(
        List<DashboardChartRawData> monthlyAverages,
        LocalDate lastUpdated,
        DashboardDataSource dataSource) {

    @Builder
    @JsonCreator
    public DashboardDataCache(List<DashboardChartRawData> monthlyAverages, LocalDate lastUpdated, DashboardDataSource dataSource) {
        this.monthlyAverages = monthlyAverages;
        this.lastUpdated = lastUpdated;
        this.dataSource = dataSource;
    }

    public static DashboardDataCache of(List<DashboardChartRawData> monthlyAverages, LocalDate lastUpdated, DashboardDataSource dataSource) {
        return DashboardDataCache.builder()
                .monthlyAverages(monthlyAverages)
                .lastUpdated(lastUpdated)
                .dataSource(dataSource)
                .build();
    }
}

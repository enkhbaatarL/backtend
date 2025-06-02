package com.sk.domain.mi.dto.response.dashboard.charts;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartRawData;

import lombok.Builder;

public record DashboardDataCache(
    List<DashboardChartRawData> monthlyAverages
    ) {

    @Builder
    @JsonCreator
    public DashboardDataCache(List<DashboardChartRawData> monthlyAverages) {
        this.monthlyAverages = monthlyAverages;
    }

    public static DashboardDataCache of(List<DashboardChartRawData> monthlyAverages) {
        return DashboardDataCache.builder()
                .monthlyAverages(monthlyAverages)
                .build();
    }
}

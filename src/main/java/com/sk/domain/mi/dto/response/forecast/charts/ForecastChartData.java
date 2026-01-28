package com.sk.domain.mi.dto.response.forecast.charts;

import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChart;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

public record ForecastChartData(
        @Schema(description = "예측 데이터의 마지막 업데이트 날짜")
        LocalDate lastUpdated,
        @Schema(description = "데이터 소스")
        ForecastDataSource dashboardDataSource,
        @Schema(description = "Forecast 데이터 소스(알림 세팅을 위한 데이터)")
        GoodsDataSourceDetail forecastGoodsDataSourceDetail,
        @Schema(description = "차트 데이터")
        ForecastChart dashboardChart) {

    @Builder
    public ForecastChartData(LocalDate lastUpdated,
            ForecastDataSource dashboardDataSource,
            GoodsDataSourceDetail forecastGoodsDataSourceDetail,
            ForecastChart dashboardChart) {
        this.lastUpdated = lastUpdated;
        this.dashboardDataSource = dashboardDataSource;
        this.forecastGoodsDataSourceDetail = forecastGoodsDataSourceDetail;
        this.dashboardChart = dashboardChart;
    }

    public static ForecastChartData of(LocalDate lastUpdated,
            ForecastDataSource dashboardDataSource,
            GoodsDataSourceDetail forecastGoodsDataSourceDetail,
            ForecastChart dashboardChart) {
        return ForecastChartData.builder()
                .lastUpdated(lastUpdated)
                .dashboardDataSource(dashboardDataSource)
                .forecastGoodsDataSourceDetail(forecastGoodsDataSourceDetail)
                .dashboardChart(dashboardChart)
                .build();
    }
}

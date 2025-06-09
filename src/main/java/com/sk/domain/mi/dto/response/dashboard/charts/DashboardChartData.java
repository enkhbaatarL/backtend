package com.sk.domain.mi.dto.response.dashboard.charts;

import java.time.LocalDate;

import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChart;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import lombok.Builder;

public record DashboardChartData(
        LocalDate lastUpdated, // 예측 데이터의 마지막 업데이트 날짜
        DashboardDataSource dashboardDataSource, // 대시보드 데이터 소스
        GoodsDataSourceDetail forecastGoodsDataSourceDetail, // Forecast 데이터 소스(알림 세팅을 위한 데이터)
        DashboardChart dashboardChart, // 차트 데이터
        boolean isFileDownload // 다운로드 가능 여부
) {

    @Builder
    public DashboardChartData(LocalDate lastUpdated,
            DashboardDataSource dashboardDataSource,
            GoodsDataSourceDetail forecastGoodsDataSourceDetail,
            DashboardChart dashboardChart,
            boolean isFileDownload) {
        this.lastUpdated = lastUpdated;
        this.dashboardDataSource = dashboardDataSource;
        this.forecastGoodsDataSourceDetail = forecastGoodsDataSourceDetail;
        this.dashboardChart = dashboardChart;
        this.isFileDownload = isFileDownload;
    }

    public static DashboardChartData of(LocalDate lastUpdated,
            DashboardDataSource dashboardDataSource,
            GoodsDataSourceDetail forecastGoodsDataSourceDetail,
            DashboardChart dashboardChart,
            boolean isFileDownload) {
        return DashboardChartData.builder()
                .lastUpdated(lastUpdated)
                .dashboardDataSource(dashboardDataSource)
                .forecastGoodsDataSourceDetail(forecastGoodsDataSourceDetail)
                .dashboardChart(dashboardChart)
                .isFileDownload(isFileDownload)
                .build();
    }
}
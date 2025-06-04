package com.sk.domain.mi.dto.response.dashboard.charts;

import java.util.List;

import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChart;
import com.sk.domain.mi.dto.response.dashboard.charts.metaData.DashboardMetadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartRes(
        @Schema(description = "메타 데이터") DashboardMetadata dashboardMetadata,

        @Schema(description = "그래프") List<DashboardChart> charts,

        @Schema(description = "알림 설정을 위한 데이터 소스 상세") List<DashboardDataSource> dashboardDataSources,

        @Schema(description = "파일 다운로드 여부") boolean isFileDownload) {

    @Builder
    public DashboardChartRes(DashboardMetadata dashboardMetadata, List<DashboardChart> charts, List<DashboardDataSource> dashboardDataSources, boolean isFileDownload) {
        this.dashboardMetadata = dashboardMetadata;
        this.charts = charts;
        this.dashboardDataSources = dashboardDataSources;
        this.isFileDownload = isFileDownload;
    }

    public static DashboardChartRes of(DashboardMetadata dashboardMetadata, List<DashboardChart> charts,
            List<DashboardDataSource> dashboardDataSources, boolean isFileDownload) {
        return DashboardChartRes.builder()
                .dashboardMetadata(dashboardMetadata)
                .charts(charts)
                .dashboardDataSources(dashboardDataSources)
                .isFileDownload(isFileDownload)
                .build();
    }
}

package com.sk.domain.mi.dto.response.dashboard;

import java.util.List;

import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChartRawData;
import com.sk.domain.mi.dto.response.dashboard.charts.metaData.DashboardChartFactorGraphMetadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartFactorGraphRes(

    @Schema(description = "요약 데이터") String summary,

    @Schema(description = "차트 메타데이터") DashboardChartFactorGraphMetadata graphMetadata,

    @Schema(description = "차트 데이터") List<DashboardChartRawData> rawData,

    @Schema(description = "차트 데이터 소스") DashboardChartFactorGraphDatasource datasource
) {
    @Builder
    public DashboardChartFactorGraphRes(String summary, DashboardChartFactorGraphMetadata graphMetadata, List<DashboardChartRawData> rawData, DashboardChartFactorGraphDatasource datasource) {
        this.summary = summary;
        this.graphMetadata = graphMetadata;
        this.rawData = rawData;
        this.datasource = datasource;
    }

    public static DashboardChartFactorGraphRes of(String summary, DashboardChartFactorGraphMetadata graphMetadata,
            List<DashboardChartRawData> rawData, DashboardChartFactorGraphDatasource datasource) {
        return DashboardChartFactorGraphRes.builder()
                .summary(summary)
                .graphMetadata(graphMetadata)
                .rawData(rawData)
                .datasource(datasource)
                .build();
    }

    public static DashboardChartFactorGraphRes of(String summary, DashboardChartFactorGraphMetadata graphMetadata) {
        return DashboardChartFactorGraphRes.builder()
                .summary(summary)
                .graphMetadata(graphMetadata)
                .build();
    }
}

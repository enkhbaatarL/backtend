package com.sk.domain.mi.dto.response.forecast;

import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChartRawData;
import com.sk.domain.mi.dto.response.forecast.charts.metaData.ForecastChartFactorGraphMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

public record ForecastChartFactorGraphRes(
        @Schema(description = "요약 데이터") String summary,
        @Schema(description = "차트 메타데이터") ForecastChartFactorGraphMetadata graphMetadata,
        @Schema(description = "차트 데이터") List<ForecastChartRawData> rawData,
        @Schema(description = "차트 데이터 소스") ForecastChartFactorGraphDatasource datasource) {
    @Builder
    public ForecastChartFactorGraphRes(String summary, ForecastChartFactorGraphMetadata graphMetadata,
            List<ForecastChartRawData> rawData, ForecastChartFactorGraphDatasource datasource) {
        this.summary = summary;
        this.graphMetadata = graphMetadata;
        this.rawData = rawData;
        this.datasource = datasource;
    }

    public static ForecastChartFactorGraphRes of(String summary,
            ForecastChartFactorGraphMetadata graphMetadata, List<ForecastChartRawData> rawData,
            ForecastChartFactorGraphDatasource datasource) {
        return ForecastChartFactorGraphRes.builder()
                .summary(summary)
                .graphMetadata(graphMetadata)
                .rawData(rawData)
                .datasource(datasource)
                .build();
    }

    public static ForecastChartFactorGraphRes of(String summary,
            ForecastChartFactorGraphMetadata graphMetadata) {
        return ForecastChartFactorGraphRes.builder()
                .summary(summary)
                .graphMetadata(graphMetadata)
                .build();
    }
}

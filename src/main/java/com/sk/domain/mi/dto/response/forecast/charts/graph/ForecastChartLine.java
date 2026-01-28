package com.sk.domain.mi.dto.response.forecast.charts.graph;

import com.sk.domain.shared.enums.chart.ChartPredictDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

public record ForecastChartLine(
        @Schema(description = "권한 여부", example = "true") Boolean isPermitted,
        @Schema(description = "차트 데이터 타입", example = "ACTUAL") ChartPredictDataType dataType,
        @Schema(description = "차트 데이터 소스 번호(주요 요인 분석 조회시 사용)", example = "1") Integer goodsDataSourceNo,
        @Schema(description = "차트 데이터") List<ForecastChartRawData> rawData,
        @Schema(description = "차트 데이터 설명") String dataExplanation) {

    @Builder
    public ForecastChartLine(Boolean isPermitted, ChartPredictDataType dataType,
            Integer goodsDataSourceNo, List<ForecastChartRawData> rawData, String dataExplanation) {
        this.isPermitted = isPermitted;
        this.dataType = dataType;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.rawData = rawData;
        this.dataExplanation = dataExplanation;
    }

    public static ForecastChartLine of(Boolean isPermitted, ChartPredictDataType dataType,
            Integer goodsDataSourceNo, List<ForecastChartRawData> rawData, String dataExplanation) {
        return ForecastChartLine.builder()
                .isPermitted(isPermitted)
                .dataType(dataType)
                .goodsDataSourceNo(goodsDataSourceNo)
                .rawData(isPermitted != null && isPermitted ? rawData : null)
                .dataExplanation(dataExplanation)
                .build();
    }
}

package com.sk.domain.mi.dto.response.forecast;

import com.sk.domain.mi.dto.response.forecast.charts.ForecastDataSource;
import com.sk.domain.mi.dto.response.forecast.charts.graph.ForecastChart;
import com.sk.domain.mi.dto.response.forecast.charts.metaData.ForecastMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Forecast 차트 기간 응답")
public record ForecastChartRes(
        @Schema(description = "x축 단위") String xAxisUnit,
        @Schema(description = "전체기간") String period,
        @Schema(description = "시작일자") String startDate,
        @Schema(description = "종료일자") String endDate,
        @Schema(description = "기간 리스트") List<ForecastChartDateItem> dateList,
        @Schema(description = "차트 목록") List<ForecastChart> charts,
        @Schema(description = "차트 데이터 소스") List<ForecastDataSource> chartsDataSource,
        @Schema(description = "다운로드 가능 여부") boolean isFileDownload,
        @Schema(description = "차트 메타데이터") ForecastMetadata metadata) {

    @Builder
    public ForecastChartRes(String xAxisUnit, String period, String startDate, String endDate,
            List<ForecastChartDateItem> dateList, List<ForecastChart> charts, List<ForecastDataSource> chartsDataSource,
            boolean isFileDownload, ForecastMetadata metadata) {
        this.xAxisUnit = xAxisUnit;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dateList = dateList;
        this.charts = charts;
        this.chartsDataSource = chartsDataSource;
        this.isFileDownload = isFileDownload;
        this.metadata = metadata;
    }

    @Builder
    public record ForecastChartDateItem(
            @Schema(description = "표시 라벨") String label,
            @Schema(description = "기간 시작일자") String startDate,
            @Schema(description = "기간 종료일자") String endDate,
            @Schema(description = "단위") String unit) {
    }
}

package com.sk.domain.mi.dto.response.forecast;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Forecast 차트 기간 응답")
public record ForecastChartRes(
        @Schema(description = "x축 단위") String xAxisUnit,
        @Schema(description = "전체기간") String period,
        @Schema(description = "시작일자") String startDate,
        @Schema(description = "종료일자") String endDate,
        @Schema(description = "기간 리스트") List<ForecastChartDateItem> dateList) {

    @Builder
    public ForecastChartRes(String xAxisUnit, String period, String startDate, String endDate,
            List<ForecastChartDateItem> dateList) {
        this.xAxisUnit = xAxisUnit;
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dateList = dateList;
    }

    @Builder
    public record ForecastChartDateItem(
            @Schema(description = "표시 라벨") String label,
            @Schema(description = "기간 시작일자") String startDate,
            @Schema(description = "기간 종료일자") String endDate,
            @Schema(description = "단위") String unit) {
    }
}

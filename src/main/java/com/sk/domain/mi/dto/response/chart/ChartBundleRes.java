package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.Interval;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record ChartBundleRes(

    ChartMetadata chartMetaData,
    DynamicChartRes mainChart,
    List<IndicatorRes> indicators,
    List<DynamicChartRes> compareCharts

) {

    @Builder
    public ChartBundleRes(ChartMetadata chartMetaData, DynamicChartRes mainChart, List<IndicatorRes> indicators, List<DynamicChartRes> compareCharts) {
        this.chartMetaData = chartMetaData;
        this.mainChart = mainChart;
        this.indicators = indicators;
        this.compareCharts = compareCharts;
    }

    public record ChartMetadata(

        @Schema(description = "값 단위", example = "$/MMBtu")
        String unit,

        @Schema(description = "interval (DAY, WEEK, MONTH)", example = "D")
        Interval interval

    ) {

    }

}

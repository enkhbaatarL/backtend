package com.sk.domain.mi.dto.response.charts;

import java.util.List;

public record ChartBundle(

    ChartMetadata chartMetadata,
    ChartRes mainChart,
    List<ChartRes> comparisonCharts,
    List<IndicatorRes> indicators

) {
}

package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.ChartType;
import lombok.Builder;

import java.util.List;

@Builder
public record Chart(

    Integer goodsDataSourceNo,
    String goodsDataSourceName,
    String goodDataName,
    String unitCd,
    ChartType chartType,
    List<String> timestamps,
    List<Series> series

) {

    public static Chart of(Integer goodsDataSourceNo, String goodsDataSourceName, String goodDataName, String unitCd, ChartType chartType, List<String> timestamps, List<Series> series) {
        return Chart.builder()
            .goodsDataSourceNo(goodsDataSourceNo)
            .goodsDataSourceName(goodsDataSourceName)
            .goodDataName(goodDataName)
            .unitCd(unitCd)
            .chartType(chartType)
            .timestamps(timestamps)
            .series(series)
            .build();
    }

}

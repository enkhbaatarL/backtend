package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.ChartType;
import lombok.Builder;

import java.util.List;

public record ChartRes(

    Integer goodsDataSourceNo,
    String unitCd, // todo 단위 비교군 데이터를 위함...
    ChartType chartType,
    List<String> timestamps,
    List<Series> series

) {

    @Builder
    public ChartRes(Integer goodsDataSourceNo, String unitCd, ChartType chartType, List<String> timestamps, List<Series> series) {
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.unitCd = unitCd;
        this.chartType = chartType;
        this.timestamps = timestamps;
        this.series = series;
    }

    public static ChartRes of(Integer goodsDataSourceNo, ChartType chartType, List<String> timestamps, List<Series> series) {
        return ChartRes.builder()
            .goodsDataSourceNo(goodsDataSourceNo)
            .chartType(chartType)
            .timestamps(timestamps)
            .series(series)
            .build();
    }

}

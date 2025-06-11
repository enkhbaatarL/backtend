package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartType;
import lombok.Builder;

import java.util.List;

@Builder
public record Chart(

    Integer goodsNo,
    Integer goodsDataNo,
    String goodsDataName,
    Integer goodsDataSourceNo,
    String goodsDataSourceName,
    String unitCd,
    ChartType chartType,
    List<String> timestamps,
    List<Series> series

) {

    public static Chart of(GoodsDataSourceDetail gds, String unitCd, ChartType chartType, List<String> timestamps, List<Series> series) {

        Integer gdsNo = gds.getGoodsDataSourceNo();
        String goodsDataSourceName = gds.getGoodsDataSourceNm();
        Integer gdNo = gds.getGoodsData().getGoodsDataNo();
        String goodsDataName = gds.getGoodsData().getGoodsDataNm();
        Integer gNo = gds.getGoodsData().getGoods().getGoodsNo();

        return Chart.builder()
            .goodsDataSourceNo(gdsNo)
            .goodsDataSourceName(goodsDataSourceName)
            .goodsDataNo(gdNo)
            .goodsDataName(goodsDataName)
            .goodsNo(gNo)
            .unitCd(unitCd)
            .chartType(chartType)
            .timestamps(timestamps)
            .series(series)
            .build();
    }

    public static Chart of(Integer goodsDataSourceNo, String goodsDataSourceName, Integer goodsDataNo, String goodsDataName, Integer goodsNo, String unitCd, ChartType chartType, List<String> ts, List<Series> ss) {
        return Chart.builder()
            .goodsDataSourceNo(goodsDataSourceNo)
            .goodsDataSourceName(goodsDataSourceName)
            .goodsDataNo(goodsDataNo)
            .goodsDataName(goodsDataName)
            .goodsNo(goodsNo)
            .unitCd(unitCd)
            .chartType(chartType)
            .timestamps(ts)
            .series(ss)
            .build();
    }

}

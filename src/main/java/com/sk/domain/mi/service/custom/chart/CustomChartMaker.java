package com.sk.domain.mi.service.custom.chart;

import com.sk.domain.mi.dto.response.charts.Indicator;
import java.math.BigDecimal;
import java.util.List;

public interface CustomChartMaker<T> {

    String getCustomId();
    Integer getGoodsDataSourceId();
    Indicator createBollingerBandData(List<String> timestamps, List<BigDecimal> series, List<String> finalizedIndicators);
    T getDashboardChart(Integer companyNo);
}

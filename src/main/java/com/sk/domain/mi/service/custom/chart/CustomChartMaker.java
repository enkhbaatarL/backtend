package com.sk.domain.mi.service.custom.chart;

public interface CustomChartMaker<T> {

    String getCustomId();
    T getDashboardChart(Integer companyNo);
}

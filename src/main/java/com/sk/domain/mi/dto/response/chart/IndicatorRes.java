package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.IndicatorType;

import java.util.List;

public record IndicatorRes(

    List<String> columns,
    List<IndicatorBundle> indicatorBundles


) implements ChartComponents {

    @Override
    public List<String> getColumns() {
        return this.columns;
    }

    public record IndicatorBundle(

        IndicatorType indicatorType,

        int windowSize,

        Double[] rows

    ) {}


}

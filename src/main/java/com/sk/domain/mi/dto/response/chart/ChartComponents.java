package com.sk.domain.mi.dto.response.chart;

import java.util.List;

public interface ChartComponents {

    List<String> getColumns();

    default String type() {
        return this.getClass().getSimpleName();
    }

}

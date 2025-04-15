package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.enums.ChartType;
import com.sk.domain.shared.enums.ChartFieldType;
import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DynamicChartRes(

    ChartType chartType,                                // CANDLE or METRIC
    List<String> columns,                               // 실제 순서 보존용 컬럼 리스트
    Map<String, ColumnMeta> schema,                     // 컬럼별 설명 및 타입 정보
    List<LinkedHashMap<String, Object>> data            // 데이터 행 (순서 보존)

) implements ChartComponents {

    @Override
    public List<String> getColumns() {
        return this.columns;
    }

    public record ColumnMeta(

        ChartFieldType chartFieldType,
        String label

    ) {

    }

    @Builder
    public DynamicChartRes(ChartType chartType, List<String> columns, Map<String, ColumnMeta> schema, List<LinkedHashMap<String, Object>> data) {
        this.chartType = chartType;
        this.columns = columns;
        this.schema = schema;
        this.data = data;
    }

}

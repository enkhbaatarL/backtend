package com.sk.domain.mi;

import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.dto.response.chart.IndicatorRes;
import com.sk.domain.shared.enums.ChartType;
import com.sk.domain.mi.dto.response.chart.DynamicChartRes;
import com.sk.domain.shared.enums.ChartFieldType;
import com.sk.domain.shared.enums.IndicatorType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConverterTest {

    public static List<BigDecimal> extractMainSeries(DynamicChartRes chart) {
        String key = chart.columns().get(1);
        return chart.data().stream()
            .map(row -> toBigDecimal(row.get(key)))
            .toList();
    }

    public static IndicatorRes createMAIndicator(DynamicChartRes chart, List<String> dates, List<Integer> windowSizes, UnitConversionService unitConversionService) {
        List<BigDecimal> series = extractMainSeries(chart);
        List<String> columns = new ArrayList<>();
        columns.add("date");

        List<IndicatorRes.IndicatorBundle> bundles = new ArrayList<>();

        for (Integer windowSize : windowSizes) {
            String maCol = "ma" + windowSize;
            columns.add(maCol);

            List<BigDecimal> maValues = unitConversionService.convertTimeSeriesForMA(series, windowSize);

            // BigDecimal[] → Double[] 변환
            Double[] rows = maValues.stream()
                .map(BigDecimal::doubleValue)
                .toArray(Double[]::new);

            IndicatorRes.IndicatorBundle bundle = new IndicatorRes.IndicatorBundle(
                IndicatorType.MA,
                windowSize,
                rows
            );

            bundles.add(bundle);
        }

        return new IndicatorRes(columns, bundles);
    }



    public static DynamicChartRes converterChart(List<Map<String, Object>> goodsDataSourceCharts, String tableColumnListNm, Map<String, String> dataColumnsMap) {

        String[] columnArr = tableColumnListNm.split(",");
        List<String> columns = List.of(columnArr);

        // schema 구성
        Map<String, DynamicChartRes.ColumnMeta> schema = new LinkedHashMap<>();
        for (int i = 0; i < columnArr.length; i++) {
            String col = columnArr[i].trim();
            schema.put(col, inferColumnMeta(col, i, dataColumnsMap));
        }

        // 데이터 정제 (순서 유지)
        List<LinkedHashMap<String, Object>> data = goodsDataSourceCharts.stream()
            .map(row -> {
                LinkedHashMap<String, Object> orderedRow = new LinkedHashMap<>();
                for (String col : columnArr) {
                    orderedRow.put(col, row.get(col));
                }
                return orderedRow;
            })
            .toList();

        // chartType 결정
//        ChartType chartType = detectChartType(schema);

        return DynamicChartRes.builder()
            .chartType(ChartType.CANDLE) // todo 테스트
            .columns(columns)
            .schema(schema)
            .data(data)
            .build();

    }

    // 타입 추론 (간단한 규칙 기반)
    private static DynamicChartRes.ColumnMeta inferColumnMeta(String columnName, int index, Map<String, String> dataColumnsMap) {
        return switch (index) {
            case 0 -> new DynamicChartRes.ColumnMeta(ChartFieldType.DATE, dataColumnsMap.getOrDefault(columnName, "날짜"));
            case 1 -> new DynamicChartRes.ColumnMeta(ChartFieldType.OPEN, dataColumnsMap.getOrDefault(columnName, "시가"));
            case 2 -> new DynamicChartRes.ColumnMeta(ChartFieldType.CLOSE, dataColumnsMap.getOrDefault(columnName, "종가"));
            case 3 -> new DynamicChartRes.ColumnMeta(ChartFieldType.HIGH, dataColumnsMap.getOrDefault(columnName, "고가"));
            case 4 -> new DynamicChartRes.ColumnMeta(ChartFieldType.LOW, dataColumnsMap.getOrDefault(columnName, "저가"));
            default -> throw new IllegalArgumentException("CandleStick 컬럼은 5개까지만 허용됩니다: index=" + index);
        };
    }

    private static BigDecimal toBigDecimal(Object obj) {
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        if (obj instanceof String str) return new BigDecimal(str);
        return null;
    }

}
